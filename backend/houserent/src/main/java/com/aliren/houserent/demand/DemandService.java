package com.aliren.houserent.demand;

import com.aliren.core.common.BusinessException;
import com.aliren.houserent.demand.dto.DemandCreateRequest;
import com.aliren.houserent.demand.dto.DemandListQuery;
import com.aliren.houserent.demand.dto.DemandResponse;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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

    private final DemandMapper demandMapper;

    public DemandService(DemandMapper demandMapper) {
        this.demandMapper = demandMapper;
    }

    /** 发布求租需求：初始状态待匹配(0) */
    public Long create(Long publisherId, DemandCreateRequest req) {
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
        return d.getId();
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
