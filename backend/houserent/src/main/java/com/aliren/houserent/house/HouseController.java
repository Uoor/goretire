package com.aliren.houserent.house;

import com.aliren.core.auth.UserContext;
import com.aliren.core.common.ApiResponse;
import com.aliren.houserent.admin.AdminAuditService;
import com.aliren.houserent.house.dto.HouseCreateRequest;
import com.aliren.houserent.house.dto.HouseListQuery;
import com.aliren.houserent.house.dto.HouseResponse;
import com.aliren.houserent.house.dto.PageDto;
import com.aliren.houserent.report.ReportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/houses")
public class HouseController {

    private final HouseService houseService;
    private final ReportService reportService;
    private final ContactService contactService;
    private final AdminAuditService adminAuditService;

    public HouseController(HouseService houseService, ReportService reportService, ContactService contactService,
                           AdminAuditService adminAuditService) {
        this.houseService = houseService;
        this.reportService = reportService;
        this.contactService = contactService;
        this.adminAuditService = adminAuditService;
    }

    /** 房源列表：仅已上架+在租（分页） */
    @GetMapping
    public ApiResponse<PageDto<HouseResponse>> list(HouseListQuery query) {
        return ApiResponse.ok(houseService.list(query));
    }

    /** 房源详情：附发布人信息 */
    @GetMapping("/{id}")
    public ApiResponse<HouseResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(houseService.detail(id));
    }

    /** 我的发布（全部状态） */
    @GetMapping("/mine")
    public ApiResponse<List<HouseResponse>> mine() {
        return ApiResponse.ok(houseService.mine(UserContext.requireUserId()));
    }

    /** 发布房源 */
    @PostMapping
    public ApiResponse<Long> publish(@Valid @RequestBody HouseCreateRequest req) {
        Long id = houseService.publish(UserContext.requireUserId(), req);
        return ApiResponse.ok(id);
    }

    /** 轻问句回答（下架时选填，仅发布人）：0=跳过 1=找到新家 2=暂无 */
    @PostMapping("/{id}/feedback")
    public ApiResponse<Void> feedback(@PathVariable Long id, @RequestBody FeedbackRequest req) {
        houseService.feedback(UserContext.requireUserId(), id, req.getAnswer());
        return ApiResponse.ok(null);
    }

    /** 举报房源 */
    @PostMapping("/{id}/reports")
    public ApiResponse<Long> report(@PathVariable Long id, @Valid @RequestBody ReportRequest req) {
        Long reportId = reportService.create(com.aliren.houserent.report.Report.TYPE_HOUSE,
                id, UserContext.requireUserId(), req.getReason());
        return ApiResponse.ok(reportId);
    }

    /** 钉钉内联系房东：返回房东钉钉身份（staffId），前端唤起单聊 */
    @PostMapping("/{id}/contact")
    public ApiResponse<com.aliren.houserent.house.dto.ContactResponse> contact(@PathVariable Long id) {
        return ApiResponse.ok(contactService.contact(UserContext.requireUserId(), id));
    }

    /** 已租出下架（发布人本人操作；管理员走 /api/admin/houses/{id}/off-rack） */
    @PostMapping("/{id}/off-rack")
    public ApiResponse<Void> offRack(@PathVariable Long id) {
        adminAuditService.offRack(UserContext.requireUserId(), UserContext.requireRole(), id);
        return ApiResponse.ok(null);
    }

    /** 删除房源（仅发布人或管理员；在租中不可删） */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> remove(@PathVariable Long id) {
        adminAuditService.deleteHouse(UserContext.requireUserId(), UserContext.requireRole(), id);
        return ApiResponse.ok(null);
    }

    /** 重新出租（状态反转）：已租出/已下架 → 在租中 */
    @PostMapping("/{id}/relist")
    public ApiResponse<Void> reList(@PathVariable Long id) {
        adminAuditService.reList(UserContext.requireUserId(), UserContext.requireRole(), id);
        return ApiResponse.ok(null);
    }

    @Data
    public static class FeedbackRequest {
        private Integer answer;
    }

    @Data
    public static class ReportRequest {
        @NotBlank(message = "举报原因不能为空")
        private String reason;
    }
}
