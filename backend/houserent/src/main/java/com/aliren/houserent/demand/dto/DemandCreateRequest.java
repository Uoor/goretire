package com.aliren.houserent.demand.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

/** 发布求租需求请求体 */
@Data
public class DemandCreateRequest {
    /** 预算区间（JSON，如 {"min":4000,"max":6000}） */
    private String budget;
    @NotBlank(message = "目标区域不能为空")
    private String region;
    /** 期望户型，如 "2室1厅" */
    private String houseType;
    /** 期望入住日期 */
    private LocalDate moveInDate;
    /** 期望租期，如 "一年" */
    private String leaseTerm;
    /** 特殊要求（JSON，如 ["可养宠","带车位"]） */
    private String requirements;
    /** 一句话描述（最多 500 字） */
    private String description;
}
