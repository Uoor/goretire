package com.aliren.houserent.match.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** 一句话找房：单套房源命中结果（含摘要字段，前端直接展示，不显示裸 id） */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HouseMatch {
    private Long houseId;
    private String reason;
    /** 小区名（前端展示用） */
    private String community;
    /** 户型 */
    private String houseType;
    /** 面积 */
    private Integer area;
    /** 月租 */
    private BigDecimal rent;
    /** 区域 */
    private String region;
}
