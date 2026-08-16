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
}
