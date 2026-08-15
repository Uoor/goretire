package com.aliren.subscribe;

import com.aliren.core.auth.UserContext;
import com.aliren.core.common.ApiResponse;
import com.aliren.subscribe.dto.SubscribeCreateRequest;
import com.aliren.subscribe.dto.SubscribeResponse;
import com.aliren.subscribe.dto.SubscribeUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscribeController {

    private final SubscribeService subscribeService;

    public SubscribeController(SubscribeService subscribeService) {
        this.subscribeService = subscribeService;
    }

    /** 创建订阅（自然语言） */
    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody SubscribeCreateRequest req) {
        return ApiResponse.ok(subscribeService.create(UserContext.requireUserId(), req));
    }

    /** 我的订阅列表 */
    @GetMapping
    public ApiResponse<List<SubscribeResponse>> list() {
        return ApiResponse.ok(subscribeService.list(UserContext.requireUserId()));
    }

    /** 修改订阅（条件/免打扰/暂停恢复） */
    @PutMapping("/{id}")
    public ApiResponse<SubscribeResponse> update(@PathVariable Long id,
                                                 @RequestBody SubscribeUpdateRequest req) {
        return ApiResponse.ok(subscribeService.update(UserContext.requireUserId(), id, req));
    }

    /** 删除订阅（软删） */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        subscribeService.delete(UserContext.requireUserId(), id);
        return ApiResponse.ok(null);
    }
}
