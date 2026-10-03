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

    /** 发布求租需求：返回需求 ID + 即时匹配的现有房源 */
    @PostMapping
    public ApiResponse<DemandService.DemandCreateResult> create(@Valid @RequestBody DemandCreateRequest req) {
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

    /** 编辑需求（仅本人）：更新字段并重新匹配一轮 */
    @PutMapping("/{id}")
    public ApiResponse<DemandResponse> update(@PathVariable Long id,
                                             @Valid @RequestBody DemandCreateRequest req) {
        return ApiResponse.ok(demandService.update(UserContext.requireUserId(), id, req));
    }

    /** 联系租客（求租墙 → 房东找租客）：返回发布者钉钉身份（staffId），前端唤起钉钉单聊 */
    @PostMapping("/{id}/contact")
    public ApiResponse<com.aliren.houserent.house.dto.ContactResponse> contact(@PathVariable Long id) {
        return ApiResponse.ok(demandService.contact(UserContext.requireUserId(), id));
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

    /** 手动重新匹配：用需求原文对现有在架房源再匹配一轮（仅本人） */
    @PostMapping("/{id}/rematch")
    public ApiResponse<com.aliren.houserent.match.dto.MatchSearchResponse> rematch(@PathVariable Long id) {
        return ApiResponse.ok(demandService.rematch(UserContext.requireUserId(), id));
    }
}
