package com.aliren.core.auth;

import com.aliren.core.common.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 钉钉免登换号（生产实现，基于钉钉开放平台新版 v1.0 API + JDK HttpClient）：
 * 1) POST /v1.0/oauth2/userAccessToken 用 appKey/appSecret + authCode 换用户级 accessToken
 * 2) GET  /v1.0/contact/users/me 用用户级 token 拿 userid（企业内部应用同组织内）
 * 参考: https://open.dingtalk.com/document/orgapp/logon-free-process
 *
 * 条件装配：仅当配置了 aliren.dingtalk.app-key 时启用（与开发桩 DingTalkClientStub 互斥）。
 */
@Slf4j
@Component
@ConditionalOnExpression("'${aliren.dingtalk.app-key:}' != ''")
public class DingTalkClientImpl implements DingTalkClient {

    private static final String USER_TOKEN_URL = "https://api.dingtalk.com/v1.0/oauth2/userAccessToken";
    private static final String USER_ME_URL = "https://api.dingtalk.com/v1.0/contact/users/me";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final String appKey;
    private final String appSecret;
    private final boolean devCodeEnabled;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DingTalkClientImpl(@Value("${aliren.dingtalk.app-key}") String appKey,
                              @Value("${aliren.dingtalk.app-secret}") String appSecret,
                              @Value("${aliren.auth.dev-code-enabled:true}") boolean devCodeEnabled) {
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.devCodeEnabled = devCodeEnabled;
        this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    }

    @Override
    public String getUserIdByCode(String authCode) {
        if (authCode == null || authCode.isBlank()) {
            throw new BusinessException(401, "免登失败");
        }
        // 本地浏览器联调桩：dev-code 直接放行（对应前端 utils/dd.js 的浏览器桩）。
        // 生产必须关闭（aliren.auth.dev-code-enabled=false），否则任何人可伪装身份。
        if (devCodeEnabled && "dev-code".equals(authCode)) {
            return authCode;
        }
        String userToken = exchangeUserToken(authCode);
        return fetchUserId(userToken);
    }

    /** authCode -> 用户级 accessToken（v1.0 oauth2） */
    private String exchangeUserToken(String authCode) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("clientId", appKey);
            body.put("clientSecret", appSecret);
            body.put("code", authCode);
            body.put("grantType", "authorization_code");
            HttpRequest request = HttpRequest.newBuilder(URI.create(USER_TOKEN_URL))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());
            if (response.statusCode() != 200 || !json.hasNonNull("accessToken")) {
                log.warn("dingtalk userAccessToken failed: status={} body={}", response.statusCode(), response.body());
                throw new BusinessException(401, "免登失败");
            }
            return json.path("accessToken").asText();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("dingtalk userAccessToken exception", e);
            throw new BusinessException(401, "免登失败");
        }
    }

    /** 用户级 token -> userid（企业内部应用同组织内；取 userId 失败时回退 unionId） */
    private String fetchUserId(String userToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(USER_ME_URL))
                    .timeout(TIMEOUT)
                    .header("x-acs-dingtalk-access-token", userToken)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());
            if (response.statusCode() != 200) {
                log.warn("dingtalk users/me failed: status={} body={}", response.statusCode(), response.body());
                throw new BusinessException(401, "免登失败");
            }
            String userId = json.path("userId").asText("");
            String unionId = json.path("unionId").asText("");
            String id = !userId.isBlank() ? userId : unionId;
            if (id.isBlank()) {
                throw new BusinessException(401, "免登失败");
            }
            return id;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("dingtalk users/me exception", e);
            throw new BusinessException(401, "免登失败");
        }
    }
}
