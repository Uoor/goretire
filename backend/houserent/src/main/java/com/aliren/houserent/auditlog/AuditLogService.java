package com.aliren.houserent.auditlog;

import com.aliren.core.common.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

/**
 * 审计日志服务：管理动作统一留痕，供信任机制与追溯。
 */
@Service
public class AuditLogService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

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
        log.setDetail(toJson(detail));
        auditLogMapper.insert(log);
    }

    /**
     * audit_log.detail 是 MySQL JSON 列，直接写自由文本会报 ER_INVALID_JSON_TEXT
     * 并导致上层事务回滚（如审核驳回/举报处理）；统一序列化为合法 JSON 字符串。
     */
    private String toJson(String detail) {
        if (detail == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(detail);
        } catch (JsonProcessingException e) {
            // 字符串序列化不会失败，兜底防御
            return "\"\"";
        }
    }

    public void requireOperator(Long operatorId) {
        if (operatorId == null) {
            throw new BusinessException(401, "未登录");
        }
    }
}
