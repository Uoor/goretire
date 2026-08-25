package com.aliren.recruit.dingtalk;

import com.aliren.core.common.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 钉钉 access_token 获取与缓存。
 * 基于 gettoken（客户端凭证，appKey+appSecret），按应用凭证分别缓存，规避频控。
 * 注意：生产环境必须为对应应用开通所需权限（如多维表 {@code Notable.Base.Read}），
 * 否则 gettoken 拿到的 token 调用会返回 AccessTokenPermissionDenied。
 */
@Slf4j
@Component
public class DingTalkTokenClient {

    private static final String GET_TOKEN_URL = "https://oapi.dingtalk.com/gettoken";
    /** access_token 官方有效期 7200s，提前 200s 过期 */
    private static final long TOKEN_TTL_MS = 7000_000L;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private record CachedToken(String token, long expireAt) {}

    private final Map<String, CachedToken> cache = new ConcurrentHashMap<>();

    /** 按 (appKey, appSecret) 获取 access_token（带缓存）。 */
    public String getAccessToken(String appKey, String appSecret) {
        if (appKey == null || appKey.isBlank() || appSecret == null || appSecret.isBlank()) {
            throw new BusinessException(500, "钉钉应用凭证未配置");
        }
        String key = appKey + "::" + appSecret;
        long now = System.currentTimeMillis();
        CachedToken cached = cache.get(key);
        if (cached != null && now < cached.expireAt()) {
            return cached.token();
        }
        synchronized (cache) {
            cached = cache.get(key);
            if (cached != null && now < cached.expireAt()) {
                return cached.token();
            }
            String token = fetchAccessToken(appKey, appSecret);
            cache.put(key, new CachedToken(token, now + TOKEN_TTL_MS));
            return token;
        }
    }

    private String fetchAccessToken(String appKey, String appSecret) {
        try {
            URI uri = URI.create(GET_TOKEN_URL + "?appkey=" + appKey + "&appsecret=" + appSecret);
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(TIMEOUT).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());
            if (json.path("errcode").asInt(-1) != 0) {
                throw new BusinessException(500, "获取钉钉token失败: " + json.path("errmsg").asText());
            }
            return json.path("access_token").asText();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[recruit-token] gettoken 异常", e);
            throw new BusinessException(500, "获取钉钉token异常");
        }
    }
}
