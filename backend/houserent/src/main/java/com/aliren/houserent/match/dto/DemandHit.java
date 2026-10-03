package com.aliren.houserent.match.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 求租墙匹配：命中的求租需求 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemandHit {
    private Long demandId;
    private String reason;
}
