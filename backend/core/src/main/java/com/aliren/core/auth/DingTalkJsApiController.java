package com.aliren.core.auth;

import com.aliren.core.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 钉钉 JSAPI 签名：前端调用 dd.config 前先取签名参数。
 * 免鉴权（dd.config 发生在登录/鉴权之前）。
 */
@Slf4j
@RestController
@RequestMapping("/api/dingtalk")
public class DingTalkJsApiController {

    private final DingTalkJsApiService jsApiService;

    public DingTalkJsApiController(DingTalkJsApiService jsApiService) {
        this.jsApiService = jsApiService;
    }

    @PostMapping("/jsapi-sign")
    public ApiResponse<Map<String, Object>> sign(@Valid @RequestBody SignRequest req) {
        log.info("[dingtalk-jsapi] sign request url={}", req.getUrl());
        return ApiResponse.ok(jsApiService.sign(req.getUrl()));
    }

    @Data
    public static class SignRequest {
        /** 钉钉内访问的完整页面 URL（location.href），签名必须与它一致 */
        @NotBlank(message = "url 不能为空")
        private String url;
    }
}
