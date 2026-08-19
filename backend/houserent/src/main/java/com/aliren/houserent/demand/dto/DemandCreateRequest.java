package com.aliren.houserent.demand.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/** 发布求租需求请求体（字段长度受限：防 LLM 成本攻击，超大文本直接 400） */
@Data
public class DemandCreateRequest {
    /** 预算区间（JSON，如 {"min":4000,"max":6000}） */
    @Size(max = 50, message = "预算格式不正确")
    private String budget;
    @NotBlank(message = "目标区域不能为空")
    @Size(max = 30, message = "区域过长")
    private String region;
    /** 期望户型，如 "2室1厅" */
    @Size(max = 30, message = "户型过长")
    private String houseType;
    /** 期望入住日期 */
    private LocalDate moveInDate;
    /** 期望租期，如 "一年" */
    @Size(max = 20, message = "租期过长")
    private String leaseTerm;
    /** 特殊要求（JSON，如 ["可养宠","带车位"]） */
    @Size(max = 200, message = "特殊要求过长")
    private String requirements;
    /** 一句话描述（最多 500 字） */
    @Size(max = 500, message = "描述最多 500 字")
    private String description;
    /** 是否同时创建订阅（新房源自动提醒）：true 时发布需求并生成一条 type=1 找房源订阅 */
    private Boolean createSubscription;
}
