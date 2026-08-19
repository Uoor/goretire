package com.aliren.core.auth;

import com.aliren.core.auth.dto.AuthResponse;
import com.aliren.core.common.ApiResponse;
import com.aliren.core.common.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final String h5BaseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthController(AuthService authService,
                          @Value("${aliren.h5.base-url:}") String h5BaseUrl) {
        this.authService = authService;
        this.h5BaseUrl = h5BaseUrl == null ? "" : h5BaseUrl.trim();
    }

    @PostMapping
    public ApiResponse<AuthResponse> auth(@Valid @RequestBody AuthRequest req) {
        return ApiResponse.ok(authService.authenticate(req.getCode()));
    }

    @Data
    public static class AuthRequest {
        @NotBlank(message = "code 不能为空")
        private String code;
    }

    /**
     * 钉钉 OAuth2 网页扫码登录回调：
     * 钉钉授权页回调带 authCode → 换 userId → 签发 JWT → 302 重定向到前端（token 在 URL hash 中）。
     * state 参数：前端传递的 base64 编码 JSON（{origin, redirect?}），
     *   origin 用于支持本地开发时重定向到 localhost；redirect 为登录后回跳的 hash 路径
     *   （群卡片落地页免登：用户从卡片进入 → 扫码 → 回到原目标页而非首页）。
     * 失败路径：非组织成员（403）→ /#/login?error=not_in_org（引导加入社群）；
     *           其他错误 → /#/login?error=auth_failed。
     */
    @GetMapping("/dingtalk/callback")
    public void oauthCallback(@RequestParam String authCode,
                              @RequestParam(required = false) String state,
                              jakarta.servlet.http.HttpServletRequest request,
                              jakarta.servlet.http.HttpServletResponse response) throws IOException {
        // 诊断：记录回调参数指纹（authCode 长度/头 + 完整 state + Referer），定位兑换失败根因
        log.info("[auth] oauth callback: codeLen={} codeHead={} state={} referer={}",
                authCode == null ? -1 : authCode.length(),
                authCode != null && authCode.length() > 6 ? authCode.substring(0, 6) : (authCode == null ? "null" : authCode),
                state, request.getHeader("Referer"));
        FrontendTarget target = resolveFrontendTarget(state, request);
        AuthResponse authResult;
        try {
            authResult = authService.authenticateOAuth(authCode);
        } catch (BusinessException e) {
            // 非组织成员：引导加入社群；其余失败：通用登录失败
            String error = e.getCode() == 403 ? "not_in_org" : "auth_failed";
            log.info("[auth] oauth callback failed: error={}, code={}", error, e.getCode());
            response.sendRedirect(target.baseUrl + "/#/login?error=" + error);
            return;
        }
        // 重定向到前端，token 和用户信息放在 URL hash 中（不经过服务端日志）
        // 优先使用 state 参数（前端 origin），否则用配置的 h5BaseUrl，最后用 Referer
        String tokenEncoded = URLEncoder.encode(authResult.getToken(), StandardCharsets.UTF_8);
        String userJson = URLEncoder.encode(
                String.format("{\"userId\":%d,\"nickname\":\"%s\",\"role\":%d,\"dingtalkUserId\":\"%s\"}",
                        authResult.getUser().getUserId(),
                        authResult.getUser().getNickname(),
                        authResult.getUser().getRole(),
                        authResult.getUser().getDingtalkUserId()),
                StandardCharsets.UTF_8);
        // 携带 redirect（hash 路径）回前端 oauth-callback，扫码后跳回原目标页
        String callbackQuery = "token=" + tokenEncoded + "&user=" + userJson;
        if (target.redirect != null && !target.redirect.isBlank()) {
            callbackQuery += "&redirect=" + URLEncoder.encode(target.redirect, StandardCharsets.UTF_8);
        }
        String redirectUrl = target.baseUrl + "/#/oauth-callback?" + callbackQuery;
        response.sendRedirect(redirectUrl);
    }

    /** 前端目标：baseUrl（origin）+ 登录后回跳的 hash 路径 */
    private record FrontendTarget(String baseUrl, String redirect) {}

    /**
     * 解析前端目标：
     * 1. 优先用 state 参数（前端 base64 编码 JSON {origin, redirect?}；兼容旧格式纯 origin）
     * 2. 其次用配置的 h5BaseUrl
     * 3. 最后用 Referer（兜底）
     */
    private FrontendTarget resolveFrontendTarget(String state, jakarta.servlet.http.HttpServletRequest request) {
        // 1. state 参数
        if (state != null && !state.isBlank()) {
            try {
                String decoded = new String(Base64.getUrlDecoder().decode(state), StandardCharsets.UTF_8);
                // 新格式：JSON {"origin": "...", "redirect": "..."}
                if (decoded.trim().startsWith("{")) {
                    JsonNode node = objectMapper.readTree(decoded);
                    String origin = node.path("origin").asText("");
                    String redirect = node.path("redirect").asText("");
                    if (origin.startsWith("http://") || origin.startsWith("https://")) {
                        return new FrontendTarget(origin, redirect);
                    }
                } else {
                    // 旧格式：纯 origin
                    if (decoded.startsWith("http://") || decoded.startsWith("https://")) {
                        return new FrontendTarget(decoded, null);
                    }
                }
            } catch (Exception e) {
                // state 解码失败，忽略
            }
        }
        // 2. 配置的 h5BaseUrl
        if (!h5BaseUrl.isBlank()) {
            return new FrontendTarget(h5BaseUrl, null);
        }
        // 3. Referer 兜底
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            try {
                java.net.URI uri = new java.net.URI(referer);
                return new FrontendTarget(
                        uri.getScheme() + "://" + uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : ""),
                        null);
            } catch (Exception e) {
                // Referer 解析失败，忽略
            }
        }
        // 最终兜底
        return new FrontendTarget("", null);
    }
}
