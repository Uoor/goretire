package com.aliren.core.auth;

import com.aliren.core.common.BusinessException;

public interface DingTalkClient {
    /** 企业内部应用 H5 免登（钉钉容器内 JSAPI requestAuthCode 的 code） */
    String getUserIdByCode(String authCode);

    /**
     * 钉钉 OAuth2 网页扫码登录（浏览器环境）：
     * 用 OAuth2 授权码换用户 accessToken → 拿 unionId → 反查企业内 userId。
     */
    String getUserIdByOAuthCode(String authCode);

    /**
     * 一次性授权工具：用 OAuth2 授权码换**用户 refresh token**（仅用于初始化，
     * 如招聘模块多维表查询的用户授权）。authCode 一次性，消费后即失效。
     *
     * @return 用户 refresh token；失败抛 BusinessException
     */
    default String getRefreshTokenByOAuthCode(String authCode) {
        throw new BusinessException(401, "不支持该操作");
    }

    /**
     * 按钉钉 userid 拉取真实资料（昵称/头像/手机号）。
     * 查不到或接口失败时返回 null（调用方保留旧值即可，不阻塞登录）。
     */
    DingTalkUserProfile getUserProfile(String userId);
}
