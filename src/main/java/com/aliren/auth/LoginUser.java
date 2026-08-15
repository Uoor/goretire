package com.aliren.auth;

import lombok.Data;

@Data
public class LoginUser {
    private Long userId;
    private String dingtalkUserId;
    private int role;
}
