package com.aliren.core.auth;

import com.aliren.core.common.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 钉钉 JSAPI 签名（dd.config 授权）：
 * access_token → jsapi_ticket（缓存 7200s）→ 四字段按字段名 ASCII 升序
 * 以 key=value& 拼接后 SHA1（官方签名算法）。
 * 前端据此调用 dd.config 启用免登/单聊等 JSAPI。
 * 参考: https://open.dingtalk.com/document/orgapp/jsapi-authentication
 */
@Slf4j
@Service
public class DingTalkJsApiService {

    private static final String GET_TOKEN_URL = "https://oapi.dingtalk.com/gettoken";
    private static final String GET_TICKET_URL = "https://oapi.dingtalk.com/get_jsapi_ticket";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final long TICKET_TTL_MS = 7000_000L; // 官方 7200s，提前 200s 过期
    private static final long TOKEN_TTL_MS = 7000_000L;  // access_token 同样 7200s，避免每次签名都 gettoken

    private final String appKey;
    private final String appSecret;
    private final String agentId;
    private final String corpId;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile String cachedTicket = "";
    private volatile long ticketExpireAt = 0;
    private volatile String cachedToken = "";
    private volatile long tokenExpireAt = 0;

    public DingTalkJsApiService(@Value("${aliren.dingtalk.app-key:}") String appKey,
                                @Value("${aliren.dingtalk.app-secret:}") String appSecret,
                                @Value("${aliren.dingtalk.agent-id:}") String agentId,
                                @Value("${aliren.dingtalk.corp-id:}") String corpId) {
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.agentId = agentId;
        this.corpId = corpId;
        this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    }

    /** 生成 dd.config 所需签名参数 */
    public Map<String, Object> sign(String url) {
        return buildSignResult(getJsApiTicket(), url);
    }

    /** 给定 ticket 与 url 生成签名参数（纯计算，便于测试） */
    Map<String, Object> buildSignResult(String ticket, String url) {
        String nonceStr = UUID.randomUUID().toString().replace("-", "");
        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
        String signature = signWithTicket(ticket, nonceStr, timeStamp, url);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("agentId", agentId);
        result.put("corpId", corpId);
        result.put("timeStamp", timeStamp);
        result.put("nonceStr", nonceStr);
        result.put("signature", signature);
        return result;
    }

    /**
     * 官方签名算法：jsapi_ticket、noncestr、timestamp、url 四字段
     * 按字段名 ASCII 升序（j < n < t < u）以 key=value& 拼接后 SHA1；
     * url 的 query 部分需做一次 urldecode。
     */
    public String signWithTicket(String ticket, String nonceStr, String timeStamp, String url) {
        String plain = "jsapi_ticket=" + ticket
                + "&noncestr=" + nonceStr
                + "&timestamp=" + timeStamp
                + "&url=" + urldecodeQuery(url);
        return sha1(plain);
    }

    /** 对 url 的 query 部分做一次 urldecode（官方要求；无 query 时原样返回） */
    static String urldecodeQuery(String url) {
        if (url == null || url.isEmpty()) return url;
        int q = url.indexOf('?');
        if (q < 0 || q == url.length() - 1) return url;
        try {
            return url.substring(0, q + 1) + URLDecoder.decode(url.substring(q + 1), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return url;
        }
    }

    private String getJsApiTicket() {
        long now = System.currentTimeMillis();
        if (!cachedTicket.isEmpty() && now < ticketExpireAt) {
            return cachedTicket;
        }
        synchronized (this) {
            if (!cachedTicket.isEmpty() && now < ticketExpireAt) {
                return cachedTicket;
            }
            String accessToken = getAccessToken();
            // 注意：get_jsapi_ticket 返回字段名为 ticket（非 jsapi_ticket）
            String ticket = httpGetString(GET_TICKET_URL + "?access_token=" + accessToken, "ticket", "获取钉钉 jsapi_ticket 失败");
            cachedTicket = ticket;
            ticketExpireAt = now + TICKET_TTL_MS;
            return ticket;
        }
    }

    private String getAccessToken() {
        long now = System.currentTimeMillis();
        if (!cachedToken.isEmpty() && now < tokenExpireAt) {
            return cachedToken;
        }
        synchronized (this) {
            if (!cachedToken.isEmpty() && now < tokenExpireAt) {
                return cachedToken;
            }
            String token = httpGetString(GET_TOKEN_URL + "?appkey=" + appKey + "&appsecret=" + appSecret,
                    "access_token", "获取钉钉 token 失败");
            cachedToken = token;
            tokenExpireAt = now + TOKEN_TTL_MS;
            return token;
        }
    }

    private String httpGetString(String url, String field, String errMsg) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(TIMEOUT).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());
            if (json.path("errcode").asInt(-1) != 0) {
                log.warn("{}: errcode={} errmsg={}", errMsg, json.path("errcode"), json.path("errmsg").asText(""));
                throw new BusinessException(500, errMsg);
            }
            String value = json.path(field).asText("");
            if (value.isEmpty()) {
                throw new BusinessException(500, errMsg);
            }
            return value;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("{} 异常", errMsg, e);
            throw new BusinessException(500, errMsg);
        }
    }

    private String sha1(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("sha1 failed", e);
        }
    }
}
