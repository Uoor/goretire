package com.aliren.rent.house.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class HouseCreateRequest {
    @NotBlank(message = "小区不能为空")
    private String community;
    private String roomNo;
    @NotBlank(message = "区域不能为空")
    private String region;
    @NotBlank(message = "户型不能为空")
    private String houseType;
    @NotNull(message = "面积不能为空")
    private Integer area;
    @NotNull(message = "租金不能为空")
    @DecimalMin(value = "0.01", message = "租金不合法")
    private BigDecimal rent;
    @NotBlank(message = "押付方式不能为空")
    private String depositPay;
    @NotNull(message = "标签不能为空")
    private Integer label;
    private Integer petOk;
    private String commute;
    private String images;
    private String description;
}
