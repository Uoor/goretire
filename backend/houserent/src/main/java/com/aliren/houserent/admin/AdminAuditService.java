package com.aliren.houserent.admin;

import com.aliren.core.common.BusinessException;
import com.aliren.houserent.auditlog.AuditLogService;
import com.aliren.houserent.house.House;
import com.aliren.houserent.house.HouseMapper;
import com.aliren.houserent.report.Report;
import com.aliren.houserent.report.ReportService;
import com.aliren.houserent.robot.PublishOrchestrator;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminAuditService {

    private final HouseMapper houseMapper;
    private final AuditLogService auditLogService;
    private final ReportService reportService;
    private final PublishOrchestrator publishOrchestrator;

    public AdminAuditService(HouseMapper houseMapper, AuditLogService auditLogService,
                             ReportService reportService, PublishOrchestrator publishOrchestrator) {
        this.houseMapper = houseMapper;
        this.auditLogService = auditLogService;
        this.reportService = reportService;
        this.publishOrchestrator = publishOrchestrator;
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
            // 通过后编排：触发订阅/求租匹配 + 新上架卡片推送（同步，后续可异步化）
            publishOrchestrator.onHouseAudited(houseId);
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

    /** 待审核队列（含房号等审核敏感字段，仅管理员） */
    public List<House> pendingList() {
        QueryWrapper<House> qw = new QueryWrapper<>();
        qw.eq("audit_status", House.AUDIT_PENDING).orderByAsc("created_at");
        return houseMapper.selectList(qw);
    }

    /** 举报列表（按状态过滤，仅管理员） */
    public List<Report> listReports(Integer status) {
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

    /** 数据看板：房源总数/待审核/已上架/已租出/今日新增 */
    public Map<String, Object> stats() {
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

    private House requirePending(Long houseId) {
        House h = houseMapper.selectById(houseId);
        if (h == null || h.getAuditStatus() != House.AUDIT_PENDING) {
            throw new BusinessException(404, "房源不存在或不在待审核状态");
        }
        return h;
    }
}
