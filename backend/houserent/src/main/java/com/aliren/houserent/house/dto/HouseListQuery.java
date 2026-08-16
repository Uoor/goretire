package com.aliren.houserent.house.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class HouseListQuery {
    private String region;
    private BigDecimal minRent;
    private BigDecimal maxRent;
    private Integer label;
    private Integer petOk;
    /** 户型模糊匹配（如"两居"命中"2室1厅"/"两居"） */
    private String houseType;
    /** 新上架：true 时仅返回近 7 天创建 */
    private Boolean newOnly;
    /** 分页：页码从 1 开始（默认 1），每页条数（默认 20） */
    private Long page = 1L;
    private Long size = 20L;
}
