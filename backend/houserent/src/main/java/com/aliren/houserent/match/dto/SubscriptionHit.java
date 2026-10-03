package com.aliren.houserent.match.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 订阅批量匹配：命中的订阅 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionHit {
    private Long subscribeId;
    private String reason;
}
