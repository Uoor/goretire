package com.aliren.houserent.demand.dto;

import com.aliren.houserent.demand.Demand;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 求租需求响应体（公开字段） */
@Data
public class DemandResponse {
    private Long id;
    private Long publisherId;
    private String budget;
    private String region;
    private String houseType;
    private LocalDate moveInDate;
    private String leaseTerm;
    private String requirements;
    private String description;
    private Integer matchStatus;
    private LocalDateTime createdAt;

    public static DemandResponse from(Demand d) {
        DemandResponse r = new DemandResponse();
        r.setId(d.getId());
        r.setPublisherId(d.getPublisherId());
        r.setBudget(d.getBudget());
        r.setRegion(d.getRegion());
        r.setHouseType(d.getHouseType());
        r.setMoveInDate(d.getMoveInDate());
        r.setLeaseTerm(d.getLeaseTerm());
        r.setRequirements(d.getRequirements());
        r.setDescription(d.getDescription());
        r.setMatchStatus(d.getMatchStatus());
        r.setCreatedAt(d.getCreatedAt());
        return r;
    }
}
