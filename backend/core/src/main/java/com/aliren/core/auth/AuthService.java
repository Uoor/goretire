package com.aliren.core.auth;

import com.aliren.core.auth.dto.AuthResponse;
import com.aliren.core.common.BusinessException;
import com.aliren.core.user.User;
import com.aliren.core.user.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AuthService {

    private final UserMapper userMapper;
    private final DingTalkClient dingTalkClient;
    private final JwtUtil jwtUtil;

    public AuthService(UserMapper userMapper, DingTalkClient dingTalkClient, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.dingTalkClient = dingTalkClient;
        this.jwtUtil = jwtUtil;
    }

    @Transactional(rollbackFor = Exception.class)
    public AuthResponse authenticate(String authCode) {
        return doAuthenticate(dingTalkClient.getUserIdByCode(authCode));
    }

    /** OAuth2 网页扫码登录（浏览器环境） */
    @Transactional(rollbackFor = Exception.class)
    public AuthResponse authenticateOAuth(String authCode) {
        return doAuthenticate(dingTalkClient.getUserIdByOAuthCode(authCode));
    }

    /**
     * 一次性授权工具：OAuth2 授权码 → 用户 refresh token
     * （招聘模块多维表查询的用户授权初始化，不建登录态）。
     */
    public String getRefreshTokenByOAuthCode(String authCode) {
        return dingTalkClient.getRefreshTokenByOAuthCode(authCode);
    }

    /** 公共登录逻辑：userId → 查找/创建用户 → 签发 JWT */
    private AuthResponse doAuthenticate(String dingtalkUserId) {
        if (dingtalkUserId == null || dingtalkUserId.isBlank()) {
            throw new BusinessException(401, "登录失败");
        }
        User user = userMapper.selectByDingtalkUserId(dingtalkUserId);
        // 拉取钉钉真实资料（昵称/头像），失败返回 null 不影响登录
        DingTalkUserProfile profile = dingTalkClient.getUserProfile(dingtalkUserId);
        if (user == null) {
            user = new User();
            user.setDingtalkUserId(dingtalkUserId);
            user.setNickname(profile != null && profile.getName() != null && !profile.getName().isBlank()
                    ? profile.getName() : "校友");
            if (profile != null && profile.getAvatar() != null && !profile.getAvatar().isBlank()) {
                user.setAvatar(profile.getAvatar());
            }
            user.setRole(User.ROLE_USER);
            user.setStatus(User.STATUS_ACTIVE);
            try {
                userMapper.insert(user);
            } catch (DuplicateKeyException e) {
                // 并发注册：另一请求已创建该用户，回查后继续
                user = userMapper.selectByDingtalkUserId(dingtalkUserId);
                if (user == null) {
                    throw e;
                }
            }
        } else if (profile != null) {
            // 已注册用户：每次免登刷新真实昵称/头像（钉钉侧改名/换头像后保持同步）
            boolean changed = false;
            if (profile.getName() != null && !profile.getName().isBlank()
                    && !profile.getName().equals(user.getNickname())) {
                user.setNickname(profile.getName());
                changed = true;
            }
            if (profile.getAvatar() != null && !profile.getAvatar().isBlank()
                    && !profile.getAvatar().equals(user.getAvatar())) {
                user.setAvatar(profile.getAvatar());
                changed = true;
            }
            if (changed) {
                userMapper.updateById(user);
            }
        }
        if (user.getStatus() == null || user.getStatus() != User.STATUS_ACTIVE) {
            throw new BusinessException(403, "账号已停用");
        }
        String token = jwtUtil.generate(user.getId(), user.getDingtalkUserId(), user.getRole());
        AuthResponse.UserInfo info = new AuthResponse.UserInfo();
        info.setUserId(user.getId());
        info.setNickname(user.getNickname());
        info.setRole(user.getRole());
        info.setDingtalkUserId(user.getDingtalkUserId());
        return new AuthResponse(token, info);
    }
}
