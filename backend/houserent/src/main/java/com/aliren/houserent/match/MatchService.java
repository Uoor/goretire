package com.aliren.houserent.match;

import com.aliren.core.common.BusinessException;
import com.aliren.houserent.demand.Demand;
import com.aliren.houserent.demand.DemandMapper;
import com.aliren.houserent.house.House;
import com.aliren.houserent.house.HouseMapper;
import com.aliren.houserent.match.client.MatchClient;
import com.aliren.houserent.match.dto.DemandHit;
import com.aliren.houserent.match.dto.HouseMatch;
import com.aliren.houserent.match.dto.MatchSearchResponse;
import com.aliren.houserent.match.dto.SubscriptionHit;
import com.aliren.houserent.subscribe.Subscribe;
import com.aliren.houserent.subscribe.SubscribeMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 匹配引擎（直接 LLM，无规则层）：一句话找房 / 订阅批量匹配 / 求租墙匹配。
 * LLM 不可用或输出非法时降级为本地简单过滤，不阻断主流程。
 */
@Slf4j
@Service
public class MatchService {

    private static final int MAX_RESULTS = 5;
    private static final Pattern PRICE_PATTERN = Pattern.compile("(\\d{3,6})\\s*(元|以内|以下|内)");
    private static final String[] LABEL_KEYWORDS = {"直租", "转租", "合租"};
    /** 户型口语关键词：文本出现即作为硬条件（命中 house_type 含关键词的房源） */
    private static final String[] TYPE_KEYWORDS = {"一居", "两居", "三居", "四居", "主卧", "次卧", "合租", "整租"};

    private final HouseMapper houseMapper;
    private final DemandMapper demandMapper;
    private final SubscribeMapper subscribeMapper;
    private final MatchClient matchClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MatchService(HouseMapper houseMapper, DemandMapper demandMapper,
                        SubscribeMapper subscribeMapper, MatchClient matchClient) {
        this.houseMapper = houseMapper;
        this.demandMapper = demandMapper;
        this.subscribeMapper = subscribeMapper;
        this.matchClient = matchClient;
    }

    /** 一句话找房：自然语言 → 3-5 套房源 + 匹配理由。
     * 策略：先本地硬过滤（区域/价格/户型），只把少量候选交给 LLM 排序+理由——
     * 候选少时 LLM 不再乱选（实测全量候选易出现区域/户型看错）。
     */
    public MatchSearchResponse searchHouses(String text) {
        List<House> candidates = houseMapper.selectList(new QueryWrapper<House>()
                .eq("audit_status", House.AUDIT_ONLINE)
                .eq("rack_status", House.RACK_RENTING)
                .orderByDesc("created_at"));
        if (candidates.isEmpty()) {
            return MatchSearchResponse.of(List.of(), false);
        }
        // 本地硬过滤：先剔除明显不符的（区域/价格/户型），候选交 LLM 排序
        List<House> filtered = prefilterSearch(text, candidates);
        // 过滤后仍有候选 → LLM 排序 + 理由；过滤后为空 → 放宽到全量交 LLM（保底），仍失败则本地降级
        List<House> llmCandidates = filtered.isEmpty() ? candidates : filtered;
        String out = matchClient.complete(buildSearchPrompt(text, llmCandidates));
        List<Object> parsed = parseHits(out, llmCandidates, "houseId");
        if (parsed == null) {
            return MatchSearchResponse.of(degradeSearch(text, llmCandidates), true);
        }
        List<HouseMatch> matches = parsed.stream().map(m -> (HouseMatch) m).toList();
        return MatchSearchResponse.of(matches.size() > MAX_RESULTS ? matches.subList(0, MAX_RESULTS) : matches, false);
    }

    /**
     * 本地硬过滤：从需求文本提取可判定的约束，剔除明显不符的房源。
     * 只过滤"确定违反"的项（区域指定且不匹配、价格超上限、户型关键词不含），
     * 未指定的维度不设限，避免误杀。
     */
    private List<House> prefilterSearch(String text, List<House> candidates) {
        Integer maxRent = extractPrice(text);
        String region = extractRegion(text, candidates);
        String typeKeyword = extractTypeKeyword(text);
        List<House> out = new ArrayList<>();
        for (House h : candidates) {
            if (maxRent != null && h.getRent() != null && h.getRent().compareTo(java.math.BigDecimal.valueOf(maxRent)) > 0) {
                continue;
            }
            if (region != null && !region.isBlank() && !h.getRegion().contains(region)) {
                continue;
            }
            if (typeKeyword != null && h.getHouseType() != null && !h.getHouseType().contains(typeKeyword)) {
                continue;
            }
            out.add(h);
        }
        return out;
    }

    /**
     * 提取户型硬条件并归一化为可匹配的关键词：
     * "一居/两居/三居" → "1室/2室/3室"（命中 1室1厅 等）；
     * "主卧/次卧/合租/整租" 保持原词。无则 null 不设限。
     */
    private String extractTypeKeyword(String text) {
        for (String kw : TYPE_KEYWORDS) {
            if (text.contains(kw)) {
                return switch (kw) {
                    case "一居" -> "1室";
                    case "两居" -> "2室";
                    case "三居" -> "3室";
                    case "四居" -> "4室";
                    default -> kw;
                };
            }
        }
        return null;
    }

    /** 订阅批量匹配：新房源 → 一次调用处理全部活跃订阅（命中订阅 ID + 理由） */
    public List<SubscriptionHit> matchSubscriptions(Long houseId) {
        House house = requireOnlineHouse(houseId);
        List<Subscribe> subs = subscribeMapper.selectList(new QueryWrapper<Subscribe>()
                .eq("status", Subscribe.STATUS_ACTIVE));
        if (subs.isEmpty()) {
            return List.of();
        }
        String out = matchClient.complete(buildSubscriptionPrompt(house, subs));
        List<Object> parsed = parseHits(out, subs, "subscribeId");
        if (parsed == null) {
            return degradeSubscriptionMatch(house, subs);
        }
        return parsed.stream().map(h -> (SubscriptionHit) h).toList();
    }

    /** 求租墙匹配：新房源 → 比对求租墙（待匹配/已匹配） */
    public List<DemandHit> matchDemands(Long houseId) {
        House house = requireOnlineHouse(houseId);
        List<Demand> demands = demandMapper.selectList(new QueryWrapper<Demand>()
                .in("match_status", Demand.STATUS_PENDING, Demand.STATUS_MATCHED));
        if (demands.isEmpty()) {
            return List.of();
        }
        String out = matchClient.complete(buildDemandPrompt(house, demands));
        List<Object> parsed = parseHits(out, demands, "demandId");
        if (parsed == null) {
            return degradeDemandMatch(house, demands);
        }
        return parsed.stream().map(h -> (DemandHit) h).toList();
    }

    // ---------- prompt 组装 ----------

    private String buildSearchPrompt(String text, List<House> houses) {
        StringBuilder sb = new StringBuilder("用户需求：").append(text).append("\n候选房源：\n");
        for (int i = 0; i < houses.size(); i++) {
            House h = houses.get(i);
            sb.append(i + 1).append(". 房源ID=").append(h.getId())
                    .append(" 小区=").append(h.getCommunity())
                    .append(" 区域=").append(h.getRegion())
                    .append(" 户型=").append(h.getHouseType())
                    .append(" 面积=").append(h.getArea()).append("㎡")
                    .append(" 月租=").append(h.getRent()).append("元")
                    .append(" 押付=").append(h.getDepositPay())
                    .append(" 标签=").append(labelText(h.getLabel()))
                    .append(" 可养宠=").append(h.getPetOk() != null && h.getPetOk() == 1 ? "是" : "否")
                    .append(" 通勤=").append(h.getCommute())
                    .append(" 描述=").append(h.getDescription() == null ? "" : h.getDescription())
                    .append("\n");
        }
        sb.append("请选出最匹配的 3-5 套，只输出 JSON 数组，每项形如 {\"houseId\": 数字, \"reason\": \"简短中文理由\"}，不要输出其他文字。");
        sb.append("注意：候选房源全部为已审核在租状态，理由中不要编造房源状态（如已租出/已下架），只依据给定的字段描述。");
        return sb.toString();
    }

    private String buildSubscriptionPrompt(House house, List<Subscribe> subs) {
        StringBuilder sb = new StringBuilder("新房源：")
                .append("房源ID=").append(house.getId())
                .append(" 小区=").append(house.getCommunity())
                .append(" 区域=").append(house.getRegion())
                .append(" 户型=").append(house.getHouseType())
                .append(" 面积=").append(house.getArea()).append("㎡")
                .append(" 月租=").append(house.getRent()).append("元")
                .append(" 标签=").append(labelText(house.getLabel()))
                .append(" 可养宠=").append(house.getPetOk() != null && house.getPetOk() == 1 ? "是" : "否")
                .append(" 通勤=").append(house.getCommute())
                .append("\n活跃订阅列表：\n");
        for (int i = 0; i < subs.size(); i++) {
            Subscribe s = subs.get(i);
            sb.append(i + 1).append(". 订阅ID=").append(s.getId())
                    .append(" 类型=").append(s.getType() == Subscribe.TYPE_FIND_HOUSE ? "找房源" : "找租客")
                    .append(" 用户需求=").append(s.getRawText())
                    .append("\n");
        }
        sb.append("请判断该房源命中了哪些订阅，只输出 JSON 数组，每项形如 {\"subscribeId\": 数字, \"reason\": \"简短中文理由\"}，未命中输出 []，不要输出其他文字。");
        return sb.toString();
    }

    private String buildDemandPrompt(House house, List<Demand> demands) {
        StringBuilder sb = new StringBuilder("新房源：")
                .append("房源ID=").append(house.getId())
                .append(" 小区=").append(house.getCommunity())
                .append(" 区域=").append(house.getRegion())
                .append(" 户型=").append(house.getHouseType())
                .append(" 面积=").append(house.getArea()).append("㎡")
                .append(" 月租=").append(house.getRent()).append("元")
                .append(" 标签=").append(labelText(house.getLabel()))
                .append(" 可养宠=").append(house.getPetOk() != null && house.getPetOk() == 1 ? "是" : "否")
                .append("\n求租墙需求列表：\n");
        for (int i = 0; i < demands.size(); i++) {
            Demand d = demands.get(i);
            sb.append(i + 1).append(". 需求ID=").append(d.getId())
                    .append(" 目标区域=").append(d.getRegion())
                    .append(" 户型=").append(d.getHouseType())
                    .append(" 预算=").append(d.getBudget())
                    .append(" 要求=").append(d.getRequirements())
                    .append(" 描述=").append(d.getDescription())
                    .append("\n");
        }
        sb.append("请判断该房源匹配了哪些需求，只输出 JSON 数组，每项形如 {\"demandId\": 数字, \"reason\": \"简短中文理由\"}，未命中输出 []，不要输出其他文字。");
        return sb.toString();
    }

    // ---------- LLM 输出解析 ----------

    /** 解析 [{"xxxId":1,"reason":"..."}]；非法返回 null（调用方降级） */
    private List<Object> parseHits(String out, List<?> targets, String idField) {
        if (out == null) {
            return null;
        }
        try {
            String json = stripCodeFence(out);
            JsonNode array = objectMapper.readTree(json);
            if (!array.isArray()) {
                return null;
            }
            List<Object> hits = new ArrayList<>();
            for (JsonNode node : array) {
                long id = node.path(idField).asLong(-1);
                String reason = node.path("reason").asText("");
                if (id <= 0 || reason.isBlank()) {
                    continue;
                }
                Object hit = toHit(findTarget(targets, id), reason);
                if (hit != null) {
                    hits.add(hit);
                }
            }
            return hits;
        } catch (Exception e) {
            log.warn("parse llm output failed: {} raw={}", e.getMessage(), out);
            return null;
        }
    }

    private String stripCodeFence(String out) {
        String s = out.trim();
        if (s.startsWith("```")) {
            s = s.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("```\\s*$", "");
        }
        int start = s.indexOf('[');
        int end = s.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return s.substring(start, end + 1);
        }
        return s;
    }

    private Object findTarget(List<?> targets, long id) {
        for (Object t : targets) {
            if (t instanceof House h && h.getId() == id) return t;
            if (t instanceof Subscribe s && s.getId() == id) return t;
            if (t instanceof Demand d && d.getId() == id) return t;
        }
        return null;
    }

    private Object toHit(Object target, String reason) {
        if (target instanceof House h) return new HouseMatch(h.getId(), reason);
        if (target instanceof Subscribe s) return new SubscriptionHit(s.getId(), reason);
        if (target instanceof Demand d) return new DemandHit(d.getId(), reason);
        return null;
    }

    // ---------- 本地降级 ----------

    private List<HouseMatch> degradeSearch(String text, List<House> candidates) {
        Integer maxRent = extractPrice(text);
        String region = extractRegion(text, candidates);
        List<HouseMatch> result = new ArrayList<>();
        for (House h : candidates) {
            if (maxRent != null && h.getRent() != null && h.getRent().compareTo(java.math.BigDecimal.valueOf(maxRent)) > 0) {
                continue;
            }
            if (region != null && !h.getRegion().contains(region)) {
                continue;
            }
            result.add(new HouseMatch(h.getId(), buildFallbackReason(h, maxRent, region)));
            if (result.size() >= MAX_RESULTS) {
                break;
            }
        }
        return result;
    }

    private List<SubscriptionHit> degradeSubscriptionMatch(House house, List<Subscribe> subs) {
        List<SubscriptionHit> hits = new ArrayList<>();
        for (Subscribe s : subs) {
            if (regionHit(s.getRawText(), house.getRegion())) {
                hits.add(new SubscriptionHit(s.getId(), "新房源位于" + house.getRegion() + "，与你的订阅区域一致（本地降级匹配）"));
            }
        }
        return hits;
    }

    /** 订阅原文与区域模糊匹配：原文包含完整区域，或包含区域末尾片区词（如"杭州西溪" → "西溪"） */
    private boolean regionHit(String rawText, String region) {
        if (rawText == null || region == null || region.isBlank()) {
            return false;
        }
        if (rawText.contains(region)) {
            return true;
        }
        for (int len = Math.min(region.length(), 4); len >= 2; len--) {
            if (rawText.contains(region.substring(region.length() - len))) {
                return true;
            }
        }
        return false;
    }

    private List<DemandHit> degradeDemandMatch(House house, List<Demand> demands) {
        List<DemandHit> hits = new ArrayList<>();
        for (Demand d : demands) {
            if (house.getRegion().equals(d.getRegion())) {
                hits.add(new DemandHit(d.getId(), "新房源位于" + house.getRegion() + "，符合你的目标区域（本地降级匹配）"));
            }
        }
        return hits;
    }

    private Integer extractPrice(String text) {
        Matcher m = PRICE_PATTERN.matcher(text);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    private String extractRegion(String text, List<House> candidates) {
        for (House h : candidates) {
            String region = h.getRegion();
            if (region != null && !region.isBlank() && text.contains(region)) {
                return region;
            }
        }
        return null;
    }

    private String buildFallbackReason(House h, Integer maxRent, String region) {
        StringBuilder sb = new StringBuilder("符合");
        if (region != null) {
            sb.append("区域「").append(region).append("」");
        }
        if (maxRent != null) {
            sb.append("预算 ≤").append(maxRent).append("元");
        }
        sb.append("（本地降级匹配）");
        return sb.toString();
    }

    private String labelText(Integer label) {
        if (label == null || label < 1 || label > LABEL_KEYWORDS.length) {
            return "";
        }
        return LABEL_KEYWORDS[label - 1];
    }

    private House requireOnlineHouse(Long houseId) {
        House h = houseMapper.selectById(houseId);
        if (h == null || h.getAuditStatus() != House.AUDIT_ONLINE) {
            throw new BusinessException(404, "房源不存在或未上架");
        }
        return h;
    }
}
