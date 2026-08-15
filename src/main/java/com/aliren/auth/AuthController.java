package com.aliren.auth;

import com.aliren.auth.dto.AuthResponse;
import com.aliren.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
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
}
