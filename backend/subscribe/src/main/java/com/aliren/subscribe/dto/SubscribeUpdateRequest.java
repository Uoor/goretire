package com.aliren.subscribe.dto;

import lombok.Data;

/** 更新订阅请求体：可更新条件/免打扰，或暂停(1)/恢复(0) */
@Data
public class SubscribeUpdateRequest {
    /** 结构化条件（JSON，可选） */
    private String structuredCondition;

    /** 自然语言原文（可选，修改时同步） */
    private String rawText;

    /** 免打扰时段（JSON，可选） */
    private String quietHours;

    /** 状态：0=活跃 1=暂停（仅允许这两个值，删除走 DELETE） */
    private Integer status;
}
