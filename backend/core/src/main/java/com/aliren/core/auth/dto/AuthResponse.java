package com.aliren.core.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private UserInfo user;

    @Data
    public static class UserInfo {
        private Long userId;
        private String nickname;
        private int role;
    }
}
