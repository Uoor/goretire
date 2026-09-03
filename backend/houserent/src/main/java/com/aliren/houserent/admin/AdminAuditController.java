package com.aliren.houserent.admin;

import com.aliren.houserent.admin.dto.AuditRequest;
import com.aliren.core.auth.UserContext;
import com.aliren.core.common.ApiResponse;
import com.aliren.houserent.house.HouseResponse;
import com.aliren.houserent.report.Report;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    public AdminAuditController(AdminAuditService adminAuditService) {
        this.adminAuditService = adminAuditService;
    }

    /** 待审核队列（含房号等审核敏感字段 + 发布人昵称，仅管理员） */
    @GetMapping("/audit/pending")
    public ApiResponse<List<Map<String, Object>>> pendingList() {
        return ApiResponse.ok(adminAuditService.pendingList(UserContext.requireRole()));
    }

    /** 全部房源列表（仅管理员）：包含所有状态的房源 + 发布人昵称，供「我的发布」对管理员展示 */
    @GetMapping("/houses")
    public ApiResponse<List<HouseResponse>> allHouses() {
        return ApiResponse.ok(adminAuditService.listAllHouses(UserContext.requireRole()));
    }

    /** 审核：通过/驳回（驳回必填原因）；通过后上层编排触发订阅/求租匹配 */
    @PostMapping("/audit/{houseId}")
    public ApiResponse<Void> audit(@PathVariable Long houseId,
                                   @Valid @RequestBody AuditRequest req) {
        adminAuditService.audit(UserContext.requireUserId(), UserContext.requireRole(),
                houseId, req.isPass(), req.getReason());
        return ApiResponse.ok(null);
    }

    /** 管理员下架 */
    @PostMapping("/houses/{houseId}/off-rack")
    public ApiResponse<Void> offRack(@PathVariable Long houseId) {
        adminAuditService.offRack(UserContext.requireUserId(), UserContext.requireRole(), houseId);
        return ApiResponse.ok(null);
    }

    /** 举报列表（可按 status 过滤，仅管理员） */
    @GetMapping("/reports")
    public ApiResponse<List<Report>> reports(@RequestParam(required = false) Integer status) {
        return ApiResponse.ok(adminAuditService.listReports(UserContext.requireRole(), status));
    }

    /** 处理举报（仅管理员，result 必填） */
    @PostMapping("/reports/{id}/handle")
    public ApiResponse<Void> handleReport(@PathVariable Long id,
                                          @Valid @RequestBody HandleReportRequest req) {
        adminAuditService.handleReport(UserContext.requireUserId(), UserContext.requireRole(),
                id, req.getResult());
        return ApiResponse.ok(null);
    }

    /** 数据看板（仅管理员） */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.ok(adminAuditService.stats(UserContext.requireRole()));
    }

    @Data
    public static class HandleReportRequest {
        @NotBlank(message = "处理结果不能为空")
        private String result;
    }
}
