package com.aliren.core.auth;

public interface DingTalkClient {
    /** 企业内部应用 H5 免登（钉钉容器内 JSAPI requestAuthCode 的 code） */
    String getUserIdByCode(String authCode);

    /**
     * 钉钉 OAuth2 网页扫码登录（浏览器环境）：
     * 用 OAuth2 授权码换用户 accessToken → 拿 unionId → 反查企业内 userId。
     */
    String getUserIdByOAuthCode(String authCode);

    /**
     * 按钉钉 userid 拉取真实资料（昵称/头像/手机号）。
     * 查不到或接口失败时返回 null（调用方保留旧值即可，不阻塞登录）。
     */
    DingTalkUserProfile getUserProfile(String userId);
}
