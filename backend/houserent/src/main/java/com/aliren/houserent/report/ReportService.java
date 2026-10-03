package com.aliren.houserent.report;

import com.aliren.core.common.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 举报：用户举报（房源/用户）→ 待处理；管理员处理 → 已处理（写审计日志）。
 */
@Service
public class ReportService {

    private final ReportMapper reportMapper;

    public ReportService(ReportMapper reportMapper) {
        this.reportMapper = reportMapper;
    }

    /** 发起举报（仅允许举报房源，MVP 简化 target_type=1） */
    @Transactional
    public Long create(Integer targetType, Long targetId, Long reporterId, String reason) {
        if (targetType == null || targetType != Report.TYPE_HOUSE) {
            throw new BusinessException("暂仅支持举报房源");
        }
        if (targetId == null) {
            throw new BusinessException("举报对象不能为空");
        }
        Report r = new Report();
        r.setTargetType(targetType);
        r.setTargetId(targetId);
        r.setReporterId(reporterId);
        r.setReason(reason);
        r.setStatus(Report.STATUS_PENDING);
        reportMapper.insert(r);
        return r.getId();
    }

    /** 举报列表（可按状态过滤，供管理后台） */
    public List<Report> list(Integer status) {
        QueryWrapper<Report> qw = new QueryWrapper<>();
        if (status != null) {
            qw.eq("status", status);
        }
        qw.orderByDesc("created_at");
        return reportMapper.selectList(qw);
    }

    /** 处理举报（仅管理员，由 Controller 校验角色）：status→已处理，写审计日志 */
    @Transactional
    public void handle(Long operatorId, Long reportId, String result) {
        if (!StringUtils.hasText(result)) {
            throw new BusinessException("处理结果不能为空");
        }
        Report r = reportMapper.selectById(reportId);
        if (r == null) {
            throw new BusinessException(404, "举报不存在");
        }
        r.setStatus(Report.STATUS_HANDLED);
        r.setResult(result);
        reportMapper.updateById(r);
    }
}
