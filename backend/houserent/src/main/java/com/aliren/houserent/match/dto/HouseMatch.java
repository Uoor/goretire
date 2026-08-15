package com.aliren.houserent.match.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 一句话找房：单套房源命中结果 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HouseMatch {
    private Long houseId;
    private String reason;
}
