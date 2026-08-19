package com.aliren.core.auth;

import com.aliren.core.auth.dto.AuthResponse;
import com.aliren.core.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
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
     */
    @GetMapping("/dingtalk/callback")
    public void oauthCallback(@RequestParam String authCode,
                              jakarta.servlet.http.HttpServletResponse response) throws IOException {
        AuthResponse authResult = authService.authenticateOAuth(authCode);
        // 重定向到前端，token 和用户信息放在 URL hash 中（不经过服务端日志）
        String frontendBase = h5BaseUrl.isBlank() ? "/" : h5BaseUrl;
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
}
