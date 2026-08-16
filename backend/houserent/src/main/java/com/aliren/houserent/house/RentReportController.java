package com.aliren.houserent.house;

import com.aliren.core.common.ApiResponse;
import com.aliren.houserent.house.dto.RentReportResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 租金周报 / 区域定价参考（产品文档 4.6 / 4.3 第 2 步） */
@RestController
@RequestMapping("/api/houses/report")
public class RentReportController {

    private final RentReportService rentReportService;

    public RentReportController(RentReportService rentReportService) {
        this.rentReportService = rentReportService;
    }

    @GetMapping("/weekly")
    public ApiResponse<RentReportResponse> weekly() {
        return ApiResponse.ok(rentReportService.weekly());
    }

    /** 单区域统计：发布房源时 AI 定价参考（平均/最低/最高月租） */
    @GetMapping("/region")
    public ApiResponse<RentReportResponse.RegionStat> region(@RequestParam String region) {
        return ApiResponse.ok(rentReportService.regionStat(region));
    }
}
