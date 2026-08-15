package com.aliren.rent.house.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class HouseListQuery {
    private String region;
    private BigDecimal minRent;
    private BigDecimal maxRent;
    private Integer label;
    private Integer petOk;
}
