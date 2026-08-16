package com.aliren.houserent.guide;

import com.aliren.core.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 避坑指南（知识库）：读取钉钉知识库。
 * GET /api/guide/sections —— 板块列表
 * GET /api/guide/sections/{nodeId}/items —— 板块下条目
 * GET /api/guide/items/{nodeId}/content —— 条目正文
 */
@RestController
@RequestMapping("/api/guide")
public class GuideController {

    private final GuideService guideService;

    public GuideController(GuideService guideService) {
        this.guideService = guideService;
    }

    @GetMapping("/sections")
    public ApiResponse<List<GuideService.GuideSection>> sections() {
        return ApiResponse.ok(guideService.sections());
    }

    /** 知识库访问 URL（去钉钉知识库下载模板等） */
    @GetMapping("/space-url")
    public ApiResponse<Map<String, String>> spaceUrl() {
        return ApiResponse.ok(Map.of("url", guideService.spaceUrl()));
    }

    @GetMapping("/sections/{nodeId}/items")
    public ApiResponse<List<GuideService.GuideItem>> items(@PathVariable String nodeId) {
        return ApiResponse.ok(guideService.items(nodeId));
    }

    @GetMapping("/items/content")
    public ApiResponse<Map<String, String>> content(@RequestParam String nodeId) {
        return ApiResponse.ok(Map.of("content", guideService.content(nodeId)));
    }
}
