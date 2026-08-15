package com.aliren.subscribe.dto;

import com.aliren.subscribe.Subscribe;
import lombok.Data;

import java.time.LocalDateTime;

/** 订阅响应体 */
@Data
public class SubscribeResponse {
    private Long id;
    private Integer type;
    private String structuredCondition;
    private String rawText;
    private Integer status;
    private String quietHours;
    private Integer pushCount;
    private LocalDateTime createdAt;

    public static SubscribeResponse from(Subscribe s) {
        SubscribeResponse r = new SubscribeResponse();
        r.setId(s.getId());
        r.setType(s.getType());
        r.setStructuredCondition(s.getStructuredCondition());
        r.setRawText(s.getRawText());
        r.setStatus(s.getStatus());
        r.setQuietHours(s.getQuietHours());
        r.setPushCount(s.getPushCount());
        r.setCreatedAt(s.getCreatedAt());
        return r;
    }
}
