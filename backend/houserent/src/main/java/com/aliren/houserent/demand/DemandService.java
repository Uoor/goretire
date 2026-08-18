package com.aliren.houserent.demand;

import com.aliren.core.common.BusinessException;
import com.aliren.houserent.demand.dto.DemandCreateRequest;
import com.aliren.houserent.demand.dto.DemandListQuery;
import com.aliren.houserent.demand.dto.DemandResponse;
import com.aliren.houserent.match.MatchService;
import com.aliren.houserent.match.dto.MatchSearchResponse;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 求租需求：发布 / 求租墙 / 我的需求 / 撤回 / 已成交。
 * 状态流转：0 待匹配 → 1 已匹配（匹配引擎）→ 2 已成交（发布人标记）；撤回 = 物理删除。
 */
@Service
public class DemandService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final DemandMapper demandMapper;
    private final MatchService matchService;

    public DemandService(DemandMapper demandMapper, MatchService matchService) {
        this.demandMapper = demandMapper;
        this.matchService = matchService;
    }

    /**
     * 发布求租需求：初始状态待匹配(0)，并立即用现有在架房源匹配一轮，
     * 返回匹配结果供前端展示（"现在就有 X 套合适"），不必干等新房源。
     */
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
