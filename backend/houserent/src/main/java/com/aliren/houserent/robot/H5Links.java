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

    /**
     * 钉钉 page/link 协议链接（无 pc_slide）：
     *   dingtalk://dingtalkclient/page/link?url={urlEncode}
     * - 移动端：钉钉内置浏览器打开（不跳出 App）
     * - PC 端：唤起钉钉客户端打开（域名不暴露在浏览器地址栏）
     * 不带 pc_slide=true：PC 端不走侧边栏，行为与移动端一致。
     * url 为空时返回空串（调用方省略链接）。
     */
    public static String dingtalkLink(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        try {
            return "dingtalk://dingtalkclient/page/link?url="
                    + URLEncoder.encode(url, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return url;
        }
    }
}
