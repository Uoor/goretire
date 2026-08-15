package com.aliren.houserent.demand;

import com.aliren.core.auth.UserContext;
import com.aliren.core.common.ApiResponse;
import com.aliren.houserent.demand.dto.DemandCreateRequest;
import com.aliren.houserent.demand.dto.DemandListQuery;
import com.aliren.houserent.demand.dto.DemandResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/demands")
public class DemandController {

    private final DemandService demandService;

    public DemandController(DemandService demandService) {
        this.demandService = demandService;
    }

    /** 发布求租需求 */
    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody DemandCreateRequest req) {
        return ApiResponse.ok(demandService.create(UserContext.requireUserId(), req));
    }

    /** 求租墙 */
    @GetMapping
    public ApiResponse<List<DemandResponse>> list(DemandListQuery query) {
        return ApiResponse.ok(demandService.list(query));
    }

    /** 我的需求 */
    @GetMapping("/mine")
    public ApiResponse<List<DemandResponse>> mine() {
        return ApiResponse.ok(demandService.mine(UserContext.requireUserId()));
    }

    /** 需求详情 */
    @GetMapping("/{id}")
    public ApiResponse<DemandResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(demandService.detail(id));
    }

    /** 撤回（仅本人） */
    @PostMapping("/{id}/withdraw")
    public ApiResponse<Void> withdraw(@PathVariable Long id) {
        demandService.withdraw(UserContext.requireUserId(), id);
        return ApiResponse.ok(null);
    }

    /** 标记已成交（仅本人） */
    @PostMapping("/{id}/complete")
    public ApiResponse<Void> complete(@PathVariable Long id) {
        demandService.complete(UserContext.requireUserId(), id);
        return ApiResponse.ok(null);
    }
}
