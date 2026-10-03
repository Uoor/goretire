package com.aliren.houserent.admin.dto;

import lombok.Data;

@Data
public class AuditRequest {
    private boolean pass;
    private String reason;
}
