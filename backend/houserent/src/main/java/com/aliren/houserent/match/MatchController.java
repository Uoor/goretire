package com.aliren.houserent.match;

import com.aliren.core.common.ApiResponse;
import com.aliren.houserent.match.dto.DemandHit;
import com.aliren.houserent.match.dto.MatchSearchResponse;
import com.aliren.houserent.match.dto.SubscriptionHit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/match")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    /** 一句话找房：自然语言 → 3-5 套房源 + 匹配理由 */
    @PostMapping("/search")
    public ApiResponse<MatchSearchResponse> search(@RequestBody SearchRequest req) {
        return ApiResponse.ok(matchService.searchHouses(req.getText()));
    }

    /** 订阅批量匹配：新房源命中哪些活跃订阅（审核通过时内部触发，也可手动调用） */
    @PostMapping("/subscriptions")
    public ApiResponse<List<SubscriptionHit>> matchSubscriptions(@RequestBody HouseIdRequest req) {
        return ApiResponse.ok(matchService.matchSubscriptions(req.getHouseId()));
    }

    /** 求租墙匹配：新房源命中哪些求租需求（审核通过时内部触发，也可手动调用） */
    @PostMapping("/demands")
    public ApiResponse<List<DemandHit>> matchDemands(@RequestBody HouseIdRequest req) {
        return ApiResponse.ok(matchService.matchDemands(req.getHouseId()));
    }

    @Data
    public static class SearchRequest {
        @NotBlank(message = "找房描述不能为空")
        private String text;
    }

    @Data
    public static class HouseIdRequest {
        @NotNull(message = "houseId 不能为空")
        private Long houseId;
    }
}
