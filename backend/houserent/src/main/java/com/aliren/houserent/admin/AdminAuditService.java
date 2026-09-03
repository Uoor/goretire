package com.aliren.houserent.admin;

import com.aliren.core.common.BusinessException;
import com.aliren.houserent.auditlog.AuditLogService;
import com.aliren.houserent.house.House;
import com.aliren.houserent.house.HouseMapper;
import com.aliren.houserent.report.Report;
import com.aliren.houserent.report.ReportService;
import com.aliren.houserent.robot.PublishOrchestrator;
import com.aliren.houserent.house.dto.HouseResponse;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminAuditService {

    private static final Logger log = LoggerFactory.getLogger(AdminAuditService.class);

    private final HouseMapper houseMapper;
    private final AuditLogService auditLogService;
    private final ReportService reportService;
    private final PublishOrchestrator publishOrchestrator;
    private final com.aliren.core.user.UserMapper userMapper;

    public AdminAuditService(HouseMapper houseMapper, AuditLogService auditLogService,
                             ReportService reportService, PublishOrchestrator publishOrchestrator,
                             com.aliren.core.user.UserMapper userMapper) {
        this.houseMapper = houseMapper;
        this.auditLogService = auditLogService;
        this.reportService = reportService;
        this.publishOrchestrator = publishOrchestrator;
        this.userMapper = userMapper;
    }

    /**
     * 审核房源：通过(1) / 驳回(2)
     * 审核通过后触发「订阅批量匹配 + 求租墙匹配」（match 模块，由上层编排调用）。
     */
    @Transactional
    public void audit(Long operatorId, int operatorRole, Long houseId, boolean pass, String reason) {
        if (operatorRole != 1) {
            throw new BusinessException(403, "无权限：仅管理员可审核");
        }
        House h = requirePending(houseId);
        if (pass) {
            h.setAuditStatus(House.AUDIT_ONLINE);
            h.setAuditReason(null);
        } else {
            if (!StringUtils.hasText(reason)) {
                throw new BusinessException("驳回原因不能为空");
            }
            h.setAuditStatus(House.AUDIT_REJECTED);
            h.setAuditReason(reason);
        }
        h.setAuditorId(operatorId);
        h.setAuditTime(java.time.LocalDateTime.now());
        houseMapper.updateById(h);
        auditLogService.record(operatorId, pass ? "AUDIT_PASS" : "AUDIT_REJECT", "house",
                houseId, pass ? null : reason);
        if (pass) {
            // 通过后编排：触发订阅/求租匹配 + 新上架卡片推送。
            // 注册到事务提交后执行：推送含外部 HTTP 调用，不能持事务（占行锁/拖长事务）；
            // 无事务上下文（如单测）时降级为同步调用。
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        runOrchestrator(houseId);
                    }
                });
            } else {
                runOrchestrator(houseId);
            }
        }
    }

    /** 编排失败不影响审核结果（状态已提交），仅记录日志 */
    private void runOrchestrator(Long houseId) {
        try {
            publishOrchestrator.onHouseAudited(houseId);
        } catch (Exception e) {
            log.warn("审核通过后编排推送失败: houseId={}", houseId, e);
        }
    }

    /** 已租出下架：仅发布人本人或管理员可操作 */
    @Transactional
    public void offRack(Long operatorId, int operatorRole, Long houseId) {
        House h = houseMapper.selectById(houseId);
        if (h == null) {
            throw new BusinessException(404, "房源不存在");
        }
        boolean isOwner = h.getPublisherId().equals(operatorId);
        boolean isAdmin = operatorRole == 1;
        if (!isOwner && !isAdmin) {
            throw new BusinessException(403, "无权限：仅发布人或管理员可下架");
        }
        h.setRackStatus(House.RACK_RENTED);
        houseMapper.updateById(h);
        auditLogService.record(operatorId, "OFF_RACK", "house", houseId, null);
    }

    /** 重新出租/上架（状态反转）：已租出/已下架 → 在租中；仅发布人本人或管理员可操作 */
    @Transactional
    public void reList(Long operatorId, int operatorRole, Long houseId) {
        House h = houseMapper.selectById(houseId);
        if (h == null) {
            throw new BusinessException(404, "房源不存在");
        }
        if (h.getAuditStatus() != House.AUDIT_ONLINE) {
            throw new BusinessException(400, "仅已上架的房源可重新出租");
        }
        boolean isOwner = h.getPublisherId().equals(operatorId);
        boolean isAdmin = operatorRole == 1;
        if (!isOwner && !isAdmin) {
            throw new BusinessException(403, "无权限：仅发布人或管理员可操作");
        }
        if (h.getRackStatus() == House.RACK_RENTING) {
            return; // 已在租中，幂等
        }
        h.setRackStatus(House.RACK_RENTING);
        houseMapper.updateById(h);
        auditLogService.record(operatorId, "RELIST", "house", houseId, null);
    }

    /**
     * 删除房源（硬删除）：仅发布人本人或管理员可操作。
     * 任意状态（含上架在租中）均可直接删除——房东有完全处置权；
     * 删除后房源从列表消失、详情 404（前端已处理）。
     */
    @Transactional
    public void deleteHouse(Long operatorId, int operatorRole, Long houseId) {
        House h = houseMapper.selectById(houseId);
        if (h == null) {
            throw new BusinessException(404, "房源不存在");
        }
        boolean isOwner = h.getPublisherId().equals(operatorId);
        boolean isAdmin = operatorRole == 1;
        if (!isOwner && !isAdmin) {
            throw new BusinessException(403, "无权限：仅发布人或管理员可删除");
        }
        houseMapper.deleteById(houseId);
        auditLogService.record(operatorId, "DELETE_HOUSE", "house", houseId, null);
    }

    /** 待审核队列（含房号等审核敏感字段 + 发布人昵称，仅管理员） */
    public List<Map<String, Object>> pendingList(int operatorRole) {
        requireAdmin(operatorRole);
        QueryWrapper<House> qw = new QueryWrapper<>();
        qw.eq("audit_status", House.AUDIT_PENDING).orderByAsc("created_at");
        return houseMapper.selectList(qw).stream().map(h -> {
            Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id", h.getId());
            m.put("community", h.getCommunity());
            m.put("roomNo", h.getRoomNo());
            m.put("region", h.getRegion());
            m.put("houseType", h.getHouseType());
            m.put("area", h.getArea());
            m.put("rent", h.getRent());
            m.put("depositPay", h.getDepositPay());
            m.put("leaseTerm", h.getLeaseTerm());
            m.put("label", h.getLabel());
            m.put("petOk", h.getPetOk());
            m.put("commute", h.getCommute());
            m.put("images", h.getImages());
            m.put("description", h.getDescription());
            m.put("auditStatus", h.getAuditStatus());
            m.put("createdAt", h.getCreatedAt());
            m.put("publisherId", h.getPublisherId());
            m.put("publisherName", publisherName(h.getPublisherId()));
            return m;
        }).toList();
    }

    /** 发布人昵称（查不到返回 null，前端兜底显示"校友"） */
    private String publisherName(Long userId) {
        if (userId == null) return null;
        com.aliren.core.user.User u = userMapper.selectById(userId);
        return u == null ? null : u.getNickname();
    }

    /**
     * 全部房源列表（管理员专用）：按创建时间倒序，包含发布人昵称。
     * 前端「我的发布」对管理员展示全部房源，复用同一套操作按钮（后端已校验发布人或管理员权限）。
     */
    public List<HouseResponse> listAllHouses(int operatorRole) {
        requireAdmin(operatorRole);
        List<House> houses = houseMapper.selectList(new QueryWrapper<House>().orderByDesc("created_at"));
        return houses.stream().map(h -> {
            HouseResponse r = HouseResponse.from(h);
            if (h.getPublisherId() != null) {
                com.aliren.core.user.User u = userMapper.selectById(h.getPublisherId());
                if (u != null) {
                    r.setPublisherName(u.getNickname());
                    r.setPublisherAvatar(u.getAvatar());
                }
            }
            return r;
        }).toList();
    }

    /** 举报列表（按状态过滤，仅管理员） */
    public List<Report> listReports(int operatorRole, Integer status) {
        requireAdmin(operatorRole);
        return reportService.list(status);
    }

    /** 处理举报（仅管理员）：状态→已处理，写审计日志 */
    @Transactional
    public void handleReport(Long operatorId, int operatorRole, Long reportId, String result) {
        if (operatorRole != 1) {
            throw new BusinessException(403, "无权限：仅管理员可处理举报");
        }
        reportService.handle(operatorId, reportId, result);
        auditLogService.record(operatorId, "REPORT_HANDLE", "report", reportId, result);
    }

    /** 数据看板：房源总数/待审核/已上架/已租出/今日新增（仅管理员） */
    public Map<String, Object> stats(int operatorRole) {
        requireAdmin(operatorRole);
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", houseMapper.selectCount(null));
        stats.put("pending", houseMapper.selectCount(new QueryWrapper<House>()
                .eq("audit_status", House.AUDIT_PENDING)));
        stats.put("online", houseMapper.selectCount(new QueryWrapper<House>()
                .eq("audit_status", House.AUDIT_ONLINE)));
        stats.put("rented", houseMapper.selectCount(new QueryWrapper<House>()
                .eq("rack_status", House.RACK_RENTED)));
        stats.put("todayNew", houseMapper.selectCount(new QueryWrapper<House>()
                .ge("created_at", LocalDate.now().atStartOfDay())));
        return stats;
    }

    /** 管理端读接口统一角色校验（待审核队列含房号、举报含举报人，不可对普通用户开放） */
    private void requireAdmin(int operatorRole) {
        if (operatorRole != 1) {
            throw new BusinessException(403, "无权限：仅管理员可查看");
        }
    }

    private House requirePending(Long houseId) {
        House h = houseMapper.selectById(houseId);
        if (h == null || h.getAuditStatus() != House.AUDIT_PENDING) {
            throw new BusinessException(404, "房源不存在或不在待审核状态");
        }
        return h;
    }
}
