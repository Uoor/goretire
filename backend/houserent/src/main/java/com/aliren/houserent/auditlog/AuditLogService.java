package com.aliren.houserent.auditlog;

import com.aliren.core.common.BusinessException;
import org.springframework.stereotype.Service;

/**
 * 审计日志服务：管理动作统一留痕，供信任机制与追溯。
 */
@Service
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;

    public AuditLogService(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    public void record(Long operatorId, String action, String targetType, Long targetId, String detail) {
        AuditLog log = new AuditLog();
        log.setOperatorId(operatorId);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        auditLogMapper.insert(log);
    }

    public void requireOperator(Long operatorId) {
        if (operatorId == null) {
            throw new BusinessException(401, "未登录");
        }
    }
}
