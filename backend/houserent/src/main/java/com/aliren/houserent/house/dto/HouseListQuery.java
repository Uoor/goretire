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
    /** 新上架：true 时仅返回近 7 天创建 */
    private Boolean newOnly;
}
