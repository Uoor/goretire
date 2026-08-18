package com.aliren.core.auth;

public interface DingTalkClient {
    String getUserIdByCode(String authCode);

    /**
     * 按钉钉 userid 拉取真实资料（昵称/头像/手机号）。
     * 查不到或接口失败时返回 null（调用方保留旧值即可，不阻塞登录）。
     */
    DingTalkUserProfile getUserProfile(String userId);
}
