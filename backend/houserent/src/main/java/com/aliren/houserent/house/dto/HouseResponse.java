package com.aliren.houserent.house.dto;

import com.aliren.houserent.house.House;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class HouseResponse {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private Long id;
    private String community;
    private String region;
    private String houseType;
    private Integer area;
    private BigDecimal rent;
    private String depositPay;
    private Integer label;
    private Integer petOk;
    private String commute;
    private List<String> images;
    private String description;
    private Integer auditStatus;
    private Integer rackStatus;
    private LocalDateTime createdAt;
    /** 发布人昵称（详情接口附带，仅公开可看信息） */
    private String publisherName;
    /** 发布人头像 */
    private String publisherAvatar;

    public static HouseResponse from(House h) {
        HouseResponse r = new HouseResponse();
        r.setId(h.getId());
        r.setCommunity(h.getCommunity());
        r.setRegion(h.getRegion());
        r.setHouseType(h.getHouseType());
        r.setArea(h.getArea());
        r.setRent(h.getRent());
        r.setDepositPay(h.getDepositPay());
        r.setLabel(h.getLabel());
        r.setPetOk(h.getPetOk());
        r.setCommute(h.getCommute());
        r.setDescription(h.getDescription());
        r.setAuditStatus(h.getAuditStatus());
        r.setRackStatus(h.getRackStatus());
        r.setCreatedAt(h.getCreatedAt());
        r.setImages(parseImages(h.getImages()));
        return r;
    }

    private static List<String> parseImages(String images) {
        if (images == null || images.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(images, new TypeReference<List<String>>() {
            });
        } catch (Exception e) {
            // 兼容非 JSON 的逗号分隔写法
            return java.util.Arrays.stream(images.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).toList();
        }
    }
}
