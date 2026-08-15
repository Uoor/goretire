package com.aliren.houserent.admin;

import com.aliren.houserent.admin.dto.AuditRequest;
import com.aliren.core.auth.UserContext;
import com.aliren.core.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    public AdminAuditController(AdminAuditService adminAuditService) {
        this.adminAuditService = adminAuditService;
    }

    @PostMapping("/audit/{houseId}")
    public ApiResponse<Void> audit(@PathVariable Long houseId,
                                   @Valid @RequestBody AuditRequest req) {
        adminAuditService.audit(UserContext.requireUserId(), UserContext.requireRole(),
                houseId, req.isPass(), req.getReason());
        return ApiResponse.ok(null);
    }

    @PostMapping("/houses/{houseId}/off-rack")
    public ApiResponse<Void> offRack(@PathVariable Long houseId) {
        adminAuditService.offRack(UserContext.requireUserId(), UserContext.requireRole(), houseId);
        return ApiResponse.ok(null);
    }
}
