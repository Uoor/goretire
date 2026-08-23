package com.aliren.houserent.robot;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * H5 落地页链接构造（群卡片/工作通知共用）。
 * 落地页：h5BaseUrl + "/#/landing?redirect={hashPath}"。
 * redirect 只含前端 hash 路径（如 /house/123），不含任何凭据；
 * 落地页负责免登（有 token 直接跳，无 token 自动扫码后回跳）。
 */
public final class H5Links {

    private H5Links() {
    }

    /** 构造 landing 落地页链接；h5BaseUrl 为空时返回空串（调用方省略链接）。 */
    public static String landingUrl(String h5BaseUrl, String hashPath) {
        if (h5BaseUrl == null || h5BaseUrl.isBlank()) {
            return "";
        }
        try {
            return h5BaseUrl + "/#/landing?redirect="
                    + URLEncoder.encode(hashPath, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return h5BaseUrl + "/#/landing";
        }
    }
}
