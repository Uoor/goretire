package com.aliren.rent.admin.dto;

import lombok.Data;

@Data
public class AuditRequest {
    private boolean pass;
    private String reason;
}
