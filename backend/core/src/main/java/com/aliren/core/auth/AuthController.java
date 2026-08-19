package com.aliren.core.auth;

import com.aliren.core.auth.dto.AuthResponse;
import com.aliren.core.common.ApiResponse;
import com.aliren.core.common.BusinessException;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final String h5BaseUrl;

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
     * state 参数：前端传递的 base64 编码 origin，用于支持本地开发时重定向到 localhost。
     * 失败路径：非组织成员（403）→ /#/login?error=not_in_org（引导加入社群）；
     *           其他错误 → /#/login?error=auth_failed。
     */
    @GetMapping("/dingtalk/callback")
    public void oauthCallback(@RequestParam String authCode,
                              @RequestParam(required = false) String state,
                              jakarta.servlet.http.HttpServletRequest request,
                              jakarta.servlet.http.HttpServletResponse response) throws IOException {
        String frontendBase = resolveFrontendBase(state, request);
        AuthResponse authResult;
        try {
            authResult = authService.authenticateOAuth(authCode);
        } catch (BusinessException e) {
            // 非组织成员：引导加入社群；其余失败：通用登录失败
            String error = e.getCode() == 403 ? "not_in_org" : "auth_failed";
            log.info("[auth] oauth callback failed: error={}, code={}", error, e.getCode());
            response.sendRedirect(frontendBase + "/#/login?error=" + error);
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
        String redirectUrl = frontendBase + "/#/oauth-callback?token=" + tokenEncoded + "&user=" + userJson;
        response.sendRedirect(redirectUrl);
    }

    /**
     * 解析前端 base URL：
     * 1. 优先用 state 参数（前端传递的 base64 编码 origin，支持本地开发）
     * 2. 其次用配置的 h5BaseUrl
     * 3. 最后用 Referer（兜底）
     */
    private String resolveFrontendBase(String state, jakarta.servlet.http.HttpServletRequest request) {
        // 1. state 参数：前端 origin 的 base64 编码
        if (state != null && !state.isBlank()) {
            try {
                String origin = new String(java.util.Base64.getUrlDecoder().decode(state), StandardCharsets.UTF_8);
                if (origin.startsWith("http://") || origin.startsWith("https://")) {
                    return origin;
                }
            } catch (Exception e) {
                // state 解码失败，忽略
            }
        }
        // 2. 配置的 h5BaseUrl
        if (!h5BaseUrl.isBlank()) {
            return h5BaseUrl;
        }
        // 3. Referer 兜底
        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            try {
                java.net.URI uri = new java.net.URI(referer);
                return uri.getScheme() + "://" + uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
            } catch (Exception e) {
                // Referer 解析失败，忽略
            }
        }
        // 最终兜底
        return "";
    }
}
