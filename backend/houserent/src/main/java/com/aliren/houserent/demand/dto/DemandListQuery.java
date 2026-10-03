package com.aliren.houserent.demand.dto;

import lombok.Data;

/** 求租墙列表查询 */
@Data
public class DemandListQuery {
    /** 按区域过滤（可选） */
    private String region;
}
