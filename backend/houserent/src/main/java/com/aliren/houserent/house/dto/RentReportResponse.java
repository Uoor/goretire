package com.aliren.houserent.house.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 租金周报响应 */
@Data
public class RentReportResponse {

    /** 区域均价（在租已上架房源） */
    private List<RegionStat> regions;
    /** 本周新上架数 */
    private long weekNew;
    /** 本周找到新家校友数（feedback_answer=1） */
    private long weekRented;
    /** 在租已上架总数 */
    private long totalOnline;

    @Data
    public static class RegionStat {
        private String region;
        private BigDecimal avgRent;
        private long count;
    }
}
