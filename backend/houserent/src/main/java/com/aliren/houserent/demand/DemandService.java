package com.aliren.houserent.demand;

import com.aliren.core.common.BusinessException;
import com.aliren.core.user.User;
import com.aliren.core.user.UserMapper;
import com.aliren.houserent.demand.dto.DemandCreateRequest;
import com.aliren.houserent.demand.dto.DemandListQuery;
import com.aliren.houserent.demand.dto.DemandResponse;
import com.aliren.houserent.house.dto.ContactResponse;
import com.aliren.houserent.match.MatchService;
import com.aliren.houserent.match.dto.MatchSearchResponse;
import com.aliren.houserent.pushlog.PushLogService;
import com.aliren.houserent.subscribe.Subscribe;
import com.aliren.houserent.subscribe.SubscribeMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 求租需求：发布 / 求租墙 / 我的需求 / 撤回 / 已成交。
 * 状态流转：0 待匹配 → 1 已匹配（匹配引擎）→ 2 已成交（发布人标记）；撤回 = 物理删除。
 * 发布时可选勾选"创建订阅"：同时生成一条 type=1 找房源订阅（新房源自动提醒），
 * 订阅管理入口已并入需求新增，订阅页仅保留管理（编辑/暂停/免打扰/删除）。
 */
@Service
public class DemandService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final DemandMapper demandMapper;
    private final MatchService matchService;
    private final UserMapper userMapper;
    private final PushLogService pushLogService;
    private final SubscribeMapper subscribeMapper;

    public DemandService(DemandMapper demandMapper, MatchService matchService,
                         UserMapper userMapper, PushLogService pushLogService,
                         SubscribeMapper subscribeMapper) {
        this.demandMapper = demandMapper;
        this.matchService = matchService;
        this.userMapper = userMapper;
        this.pushLogService = pushLogService;
        this.subscribeMapper = subscribeMapper;
    }

    /**
     * 发布求租需求：初始状态待匹配(0)，并立即用现有在架房源匹配一轮，
     * 返回匹配结果供前端展示（"现在就有 X 套合适"），不必干等新房源。
     * 勾选 createSubscription 时：同时创建一条 type=1 找房源订阅（新房源自动提醒）。
     */
    @Transactional
    public DemandCreateResult create(Long publisherId, DemandCreateRequest req) {
        Demand d = new Demand();
        d.setPublisherId(publisherId);
        d.setBudget(req.getBudget());
        d.setRegion(req.getRegion());
        d.setHouseType(req.getHouseType());
        d.setMoveInDate(req.getMoveInDate());
        d.setLeaseTerm(req.getLeaseTerm());
        d.setRequirements(req.getRequirements());
        d.setDescription(req.getDescription());
        d.setMatchStatus(Demand.STATUS_PENDING);
        demandMapper.insert(d);
        // 勾选"创建订阅"：用需求的结构化字段直接生成订阅（不重复调 LLM 解析，
        // 需求字段本身已结构化；rawText 给可读的自然语言描述）
        if (req.getCreateSubscription() != null && req.getCreateSubscription()) {
            createSubscriptionForDemand(publisherId, d);
        }
        // 立即匹配现有房源：用需求要素拼一句话（区域+户型+预算+要求）
        String queryText = buildMatchQuery(req);
        MatchSearchResponse matched = matchService.searchHouses(queryText);
        // 有匹配 → 状态置为"已匹配"，前端可见；无匹配保持"待匹配"
        if (matched.getMatches() != null && !matched.getMatches().isEmpty()) {
            d.setMatchStatus(Demand.STATUS_MATCHED);
            demandMapper.updateById(d);
        }
        DemandCreateResult result = new DemandCreateResult();
        result.setDemandId(d.getId());
        result.setMatches(matched.getMatches());
        result.setDegraded(matched.isDegraded());
        return result;
    }

    /**
     * 需求 → 自动创建找房源订阅（type=1）：
     * rawText 为需求自然语言描述；structuredCondition 直接复用需求的结构化字段
     * （region/houseType/预算 JSON/要求 JSON），不再额外调用 LLM 解析。
     */
    private void createSubscriptionForDemand(Long userId, Demand d) {
        Subscribe s = new Subscribe();
        s.setUserId(userId);
        s.setType(Subscribe.TYPE_FIND_HOUSE);
        s.setRawText(buildSubscriptionText(d));
        s.setStructuredCondition(buildSubscriptionCondition(d));
        s.setStatus(Subscribe.STATUS_ACTIVE);
        s.setPushCount(0);
        subscribeMapper.insert(s);
    }

    /** 订阅原文：区域+户型+预算+要求，拼成一句话（进 LLM 匹配，复用 buildMatchQuery 的清洗逻辑） */
    private String buildSubscriptionText(Demand d) {
        DemandCreateRequest req = new DemandCreateRequest();
        req.setRegion(d.getRegion());
        req.setHouseType(d.getHouseType());
        req.setBudget(d.getBudget());
        req.setRequirements(d.getRequirements());
        req.setDescription(d.getDescription());
        String query = buildMatchQuery(req);
        return StringUtils.hasText(query) ? query : d.getRegion();
    }

    /** 订阅结构化条件 JSON：{region, minRent, maxRent, houseType, requirements}，对齐 SubscriptionParser 输出 */
    private String buildSubscriptionCondition(Demand d) {
        ObjectNode node = MAPPER.createObjectNode();
        if (StringUtils.hasText(d.getRegion())) {
            node.put("region", d.getRegion());
        }
        if (StringUtils.hasText(d.getHouseType())) {
            node.put("houseType", d.getHouseType());
        }
        if (StringUtils.hasText(d.getBudget())) {
            try {
                JsonNode b = MAPPER.readTree(d.getBudget());
                if (b.path("min").isNumber()) node.put("minRent", b.path("min").asInt());
                if (b.path("max").isNumber()) node.put("maxRent", b.path("max").asInt());
            } catch (Exception ignored) {
                // 预算非法 JSON 时省略数字，不影响订阅
            }
        }
        if (StringUtils.hasText(d.getRequirements())) {
            try {
                ArrayNode arr = MAPPER.readValue(d.getRequirements(), ArrayNode.class);
                if (arr != null && !arr.isEmpty()) {
                    node.set("requirements", arr);
                }
            } catch (Exception ignored) {
                // 要求非法 JSON 时省略，不影响订阅
            }
        }
        return node.isEmpty() ? null : node.toString();
    }

    /**
     * 手动重新匹配：用需求原文重新对现有在架房源匹配一轮。
     * 有结果 → 状态置已匹配；无结果 → 置回待匹配。返回匹配结果供展示。
     */
    public MatchSearchResponse rematch(Long userId, Long id) {
        Demand d = requireDemand(id);
        if (!d.getPublisherId().equals(userId)) {
            throw new BusinessException(403, "无权限：仅发布人可操作");
        }
        DemandCreateRequest req = new DemandCreateRequest();
        req.setRegion(d.getRegion());
        req.setHouseType(d.getHouseType());
        req.setBudget(d.getBudget());
        req.setRequirements(d.getRequirements());
        req.setDescription(d.getDescription());
        MatchSearchResponse matched = matchService.searchHouses(buildMatchQuery(req));
        if (matched.getMatches() != null && !matched.getMatches().isEmpty()) {
            d.setMatchStatus(Demand.STATUS_MATCHED);
        } else {
            d.setMatchStatus(Demand.STATUS_PENDING);
        }
        demandMapper.updateById(d);
        return matched;
    }

    /** 需求 → 一句话匹配文本（喂给一句话找房） */
    private String buildMatchQuery(DemandCreateRequest req) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(req.getRegion())) sb.append(req.getRegion()).append(" ");
        if (StringUtils.hasText(req.getHouseType())) sb.append(req.getHouseType()).append(" ");
        if (StringUtils.hasText(req.getBudget())) {
            // budget 是 JSON {"min":4000,"max":6000} → "4000-6000元"（解析失败降级取数字）
            String budget = budgetText(req.getBudget());
            if (StringUtils.hasText(budget)) {
                sb.append(budget).append(" ");
            }
        }
        if (StringUtils.hasText(req.getRequirements())) {
            String r = req.getRequirements();
            if (r.contains("养宠") || r.contains("宠物")) sb.append("可养宠 ");
            if (r.contains("车位")) sb.append("带车位 ");
        }
        if (StringUtils.hasText(req.getDescription())) sb.append(req.getDescription());
        return sb.toString().trim();
    }

    /**
     * 预算 JSON → 可读区间文本：{"min":4000,"max":6000} → "4000-6000元"。
     * 只有 max 时 → "6000元以内"；非 JSON 纯文本（如 "6000以内"）降级取其中数字。
     */
    private String budgetText(String budget) {
        String raw = budget.trim();
        try {
            JsonNode b = MAPPER.readTree(raw);
            int min = b.path("min").asInt(0);
            int max = b.path("max").asInt(0);
            if (min > 0 && max > 0) {
                return min + "-" + max + "元";
            }
            if (max > 0) {
                return max + "元以内";
            }
            if (min > 0) {
                return min + "元以上";
            }
            return "";
        } catch (Exception e) {
            // 非 JSON：纯文本预算直接取数字；非法 JSON 对象则跳过，避免拼出乱码喂给 LLM
            if (raw.startsWith("{")) {
                return "";
            }
            String digits = raw.replaceAll("[^0-9]", "");
            return digits.isEmpty() ? "" : digits + "元以内";
        }
    }

    /** 发布响应：需求 ID + 即时匹配结果 */
    public static class DemandCreateResult {
        private Long demandId;
        private List<com.aliren.houserent.match.dto.HouseMatch> matches;
        private boolean degraded;

        public Long getDemandId() { return demandId; }
        public void setDemandId(Long demandId) { this.demandId = demandId; }
        public List<com.aliren.houserent.match.dto.HouseMatch> getMatches() { return matches; }
        public void setMatches(List<com.aliren.houserent.match.dto.HouseMatch> matches) { this.matches = matches; }
        public boolean isDegraded() { return degraded; }
        public void setDegraded(boolean degraded) { this.degraded = degraded; }
    }

    /** 求租墙：仅待匹配/已匹配（不含已成交），按创建倒序，支持区域过滤 */
    public List<DemandResponse> list(DemandListQuery query) {
        QueryWrapper<Demand> qw = new QueryWrapper<>();
        qw.in("match_status", Demand.STATUS_PENDING, Demand.STATUS_MATCHED);
        if (query != null && StringUtils.hasText(query.getRegion())) {
            qw.eq("region", query.getRegion());
        }
        qw.orderByDesc("created_at");
        return demandMapper.selectList(qw).stream().map(DemandResponse::from).toList();
    }

    /** 我的需求：全部状态 */
    public List<DemandResponse> mine(Long userId) {
        QueryWrapper<Demand> qw = new QueryWrapper<>();
        qw.eq("publisher_id", userId).orderByDesc("created_at");
        return demandMapper.selectList(qw).stream().map(DemandResponse::from).toList();
    }

    /** 需求详情 */
    public DemandResponse detail(Long id) {
        Demand d = requireDemand(id);
        return DemandResponse.from(d);
    }

    /**
     * 编辑需求（仅本人）：更新各字段并重新匹配一轮（条件变了，匹配状态刷新）。
     * 返回更新后的需求；重新匹配失败不阻断保存（保持待匹配，可手动重匹配）。
     */
    @Transactional
    public DemandResponse update(Long userId, Long id, DemandCreateRequest req) {
        Demand d = requireDemand(id);
        if (!d.getPublisherId().equals(userId)) {
            throw new BusinessException(403, "无权限：仅发布人可编辑");
        }
        if (d.getMatchStatus() != null && d.getMatchStatus() == Demand.STATUS_DONE) {
            throw new BusinessException(400, "已成交的需求不可编辑");
        }
        d.setBudget(req.getBudget());
        d.setRegion(req.getRegion());
        d.setHouseType(req.getHouseType());
        d.setMoveInDate(req.getMoveInDate());
        d.setLeaseTerm(req.getLeaseTerm());
        d.setRequirements(req.getRequirements());
        d.setDescription(req.getDescription());
        demandMapper.updateById(d);
        // 条件变化 → 重新匹配一轮，刷新匹配状态（LLM 不可用自动降级，不阻断保存）
        DemandCreateRequest matchReq = new DemandCreateRequest();
        matchReq.setRegion(d.getRegion());
        matchReq.setHouseType(d.getHouseType());
        matchReq.setBudget(d.getBudget());
        matchReq.setRequirements(d.getRequirements());
        matchReq.setDescription(d.getDescription());
        MatchSearchResponse matched = matchService.searchHouses(buildMatchQuery(matchReq));
        if (matched.getMatches() != null && !matched.getMatches().isEmpty()) {
            d.setMatchStatus(Demand.STATUS_MATCHED);
        } else {
            d.setMatchStatus(Demand.STATUS_PENDING);
        }
        demandMapper.updateById(d);
        return DemandResponse.from(d);
    }

    /**
     * 联系租客（求租墙 → 房东找租客闭环）：返回发布者钉钉身份（staffId），
     * 前端唤起钉钉单聊沟通。不留手机号，钉钉内联系（对齐房源侧 ContactService）。
     */
    public ContactResponse contact(Long contactorId, Long demandId) {
        Demand d = requireDemand(demandId);
        if (d.getMatchStatus() != null && d.getMatchStatus() == Demand.STATUS_DONE) {
            throw new BusinessException(400, "该需求已成交，无需联系");
        }
        User publisher = userMapper.selectById(d.getPublisherId());
        if (publisher == null || !StringUtils.hasText(publisher.getDingtalkUserId())) {
            throw new BusinessException(500, "租客暂未开通钉钉身份，请稍后再试");
        }
        pushLogService.record(contactorId, null, null,
                "发起联系求租需求：「" + d.getRegion() + "」" + (d.getHouseType() == null ? "" : d.getHouseType())
                        + "（租客 " + publisher.getNickname() + "）");
        return new ContactResponse(publisher.getNickname(), publisher.getDingtalkUserId());
    }

    /** 撤回（仅本人）：物理删除 */
    @Transactional
    public void withdraw(Long userId, Long id) {
        Demand d = requireDemand(id);
        if (!d.getPublisherId().equals(userId)) {
            throw new BusinessException(403, "无权限：仅发布人可撤回");
        }
        demandMapper.deleteById(id);
    }

    /** 标记已成交（仅本人）：待匹配/已匹配 → 已成交 */
    @Transactional
    public void complete(Long userId, Long id) {
        Demand d = requireDemand(id);
        if (!d.getPublisherId().equals(userId)) {
            throw new BusinessException(403, "无权限：仅发布人可操作");
        }
        d.setMatchStatus(Demand.STATUS_DONE);
        demandMapper.updateById(d);
    }

    private Demand requireDemand(Long id) {
        Demand d = demandMapper.selectById(id);
        if (d == null) {
            throw new BusinessException(404, "需求不存在");
        }
        return d;
    }
}
