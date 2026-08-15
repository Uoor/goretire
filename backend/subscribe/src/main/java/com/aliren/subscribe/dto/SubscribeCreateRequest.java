package com.aliren.subscribe.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 创建订阅请求体（自然语言订阅，MVP 阶段结构化条件由前端可选传入或后续 LLM 解析填充） */
@Data
public class SubscribeCreateRequest {
    /** 订阅类型：1=找房源 2=找租客 */
    @NotNull(message = "订阅类型不能为空")
    @Min(value = 1, message = "订阅类型不合法")
    @Max(value = 2, message = "订阅类型不合法")
    private Integer type;

    /** 自然语言原文（如"西溪附近 6000 以内两居，能养猫"） */
    @NotBlank(message = "订阅内容不能为空")
    private String rawText;

    /** 结构化条件（JSON，可选；MVP 可省略，后续由 LLM 解析填充） */
    private String structuredCondition;

    /** 免打扰时段（JSON，可选，如 {"start":"22:00","end":"08:00"}） */
    private String quietHours;
}
