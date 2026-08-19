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
 * 钉钉免登换号（生产实现，企业内部应用 H5 微应用免登链路）：
 * 1) GET  oapi.dingtalk.com/gettoken?appkey=&appsecret= 拿应用级 access_token（7200s 有效）
 * 2) POST oapi.dingtalk.com/topapi/v2/user/getuserinfo?access_token=  body {code}
 *    用前端 requestAuthCode 的临时授权码换 userid（授权码 5 分钟内有效，且只能使用一次）
 * 参考: https://open.dingtalk.com/document/orgapp/logon-free-process
 *
 * 注意：这是企业内部应用（agentId 体系）的免登链路。第三方应用/新版应用走
 * v1.0/oauth2/userAccessToken + /v1.0/contact/users/me，两者不可混用。
 *
 * 条件装配：仅当配置了 aliren.dingtalk.app-key 时启用（与开发桩 DingTalkClientStub 互斥）。
 */
@Slf4j
@Component
@ConditionalOnExpression("'${aliren.dingtalk.app-key:}' != ''")
public class DingTalkClientImpl implements DingTalkClient {

    private static final String GET_TOKEN_URL = "https://oapi.dingtalk.com/gettoken";
    private static final String GET_USERINFO_URL = "https://oapi.dingtalk.com/topapi/v2/user/getuserinfo";
    private static final String GET_USER_URL = "https://oapi.dingtalk.com/topapi/v2/user/get";
    private static final String GET_USER_BY_UNIONID_URL = "https://oapi.dingtalk.com/topapi/user/getbyunionid";
    // OAuth2 网页扫码登录（新版 API）
    private static final String OAUTH2_TOKEN_URL = "https://api.dingtalk.com/v1.0/oauth2/userAccessToken";
    private static final String OAUTH2_USER_ME_URL = "https://api.dingtalk.com/v1.0/contact/users/me";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final long TOKEN_TTL_MS = 7000_000L; // access_token 7200s，提前 200s 过期

    private final String appKey;
    private final String appSecret;
    private final boolean devCodeEnabled;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile String cachedToken = "";
    private volatile long tokenExpireAt = 0;

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
        String accessToken = getAccessToken();
        return fetchUserIdByCode(authCode, accessToken);
    }

    /** appKey/appSecret -> 应用级 access_token（缓存 7200s） */
    private String getAccessToken() {
        long now = System.currentTimeMillis();
        if (!cachedToken.isEmpty() && now < tokenExpireAt) {
            return cachedToken;
        }
        synchronized (this) {
            if (!cachedToken.isEmpty() && now < tokenExpireAt) {
                return cachedToken;
            }
            try {
                HttpRequest request = HttpRequest.newBuilder(
                                URI.create(GET_TOKEN_URL + "?appkey=" + appKey + "&appsecret=" + appSecret))
                        .timeout(TIMEOUT)
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode json = objectMapper.readTree(response.body());
                if (response.statusCode() != 200 || json.path("errcode").asInt(0) != 0
                        || !json.hasNonNull("access_token")) {
                    log.warn("dingtalk gettoken failed: status={} body={}", response.statusCode(), response.body());
                    throw new BusinessException(401, "免登失败");
                }
                cachedToken = json.path("access_token").asText();
                tokenExpireAt = now + TOKEN_TTL_MS;
                return cachedToken;
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.warn("dingtalk gettoken exception", e);
                throw new BusinessException(401, "免登失败");
            }
        }
    }

    /** 临时授权码 code + access_token -> userid（企业内部应用免登） */
    private String fetchUserIdByCode(String authCode, String accessToken) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("code", authCode);
            HttpRequest request = HttpRequest.newBuilder(URI.create(GET_USERINFO_URL + "?access_token=" + accessToken))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());
            if (response.statusCode() != 200 || json.path("errcode").asInt(0) != 0) {
                log.warn("dingtalk getuserinfo failed: status={} body={}", response.statusCode(), response.body());
                throw new BusinessException(401, "免登失败");
            }
            JsonNode result = json.path("result");
            String userId = result.path("userid").asText("");
            if (userId.isBlank()) {
                log.warn("dingtalk getuserinfo empty userid: body={}", response.body());
                throw new BusinessException(401, "免登失败");
            }
            return userId;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("dingtalk getuserinfo exception", e);
            throw new BusinessException(401, "免登失败");
        }
    }

    @Override
    public String getUserIdByOAuthCode(String authCode) {
        if (authCode == null || authCode.isBlank()) {
            throw new BusinessException(401, "扫码登录失败");
        }
        // 1. OAuth2 code → 用户级 accessToken
        // 注意：不要先用 getuserinfo 尝试——OAuth2 授权码是一次性的，
        // 被任何接口消费后即失效，必须直接用于 userAccessToken 兑换。
        String userAccessToken = exchangeOAuthCode(authCode);
        // 2. 用户 accessToken → /contact/users/me → unionId
        String unionId = getUnionIdByUserToken(userAccessToken);
        // 3. unionId + 应用 accessToken → /topapi/user/getbyunionid → userId
        return getUserIdByUnionId(unionId);
    }

    /** OAuth2 授权码换用户 accessToken */
    private String exchangeOAuthCode(String authCode) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("clientId", appKey);
            body.put("clientSecret", appSecret);
            body.put("code", authCode);
            body.put("grantType", "authorization_code");
            HttpRequest request = HttpRequest.newBuilder(URI.create(OAUTH2_TOKEN_URL))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());
            String accessToken = json.path("accessToken").asText("");
            if (accessToken.isEmpty()) {
                // 诊断：记录 authCode 指纹与完整响应，定位兑换失败原因
                log.warn("dingtalk oauth2 token failed: status={} codeLen={} codeHead={} clientId={} body={}",
                        response.statusCode(),
                        authCode == null ? -1 : authCode.length(),
                        authCode != null && authCode.length() > 6 ? authCode.substring(0, 6) : (authCode == null ? "null" : authCode),
                        appKey, response.body());
                throw new BusinessException(401, "扫码登录失败");
            }
            return accessToken;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("dingtalk oauth2 token exception", e);
            throw new BusinessException(401, "扫码登录失败");
        }
    }

    /** 用户 accessToken → /contact/users/me → unionId */
    private String getUnionIdByUserToken(String userAccessToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(OAUTH2_USER_ME_URL))
                    .timeout(TIMEOUT)
                    .header("x-acs-dingtalk-access-token", userAccessToken)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());
            String unionId = json.path("unionId").asText("");
            if (unionId.isEmpty()) {
                log.warn("dingtalk /contact/users/me empty unionId: status={} body={}", response.statusCode(), response.body());
                throw new BusinessException(401, "扫码登录失败");
            }
            return unionId;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("dingtalk /contact/users/me exception", e);
            throw new BusinessException(401, "扫码登录失败");
        }
    }

    /**
     * unionId + 应用 accessToken → /topapi/user/getbyunionid → userId
     *
     * 关键语义：反查的是「应用所属企业」内的 userid。用户不在应用所属企业
     * （即非「阿里人·一起提前退休」社群组织成员）时，此接口报错/返回空，
     * 抛 403 供上层区分"非组织成员"（引导加入社群）与普通登录失败。
     */
    private String getUserIdByUnionId(String unionId) {
        try {
            String accessToken = getAccessToken();
            ObjectNode body = objectMapper.createObjectNode();
            body.put("unionid", unionId);
            HttpRequest request = HttpRequest.newBuilder(URI.create(GET_USER_BY_UNIONID_URL + "?access_token=" + accessToken))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());
            if (response.statusCode() != 200 || json.path("errcode").asInt(0) != 0) {
                log.warn("dingtalk getbyunionid failed: status={} body={}", response.statusCode(), response.body());
                throw new BusinessException(403, "非组织成员");
            }
            String userId = json.path("result").path("userid").asText("");
            if (userId.isBlank()) {
                log.warn("dingtalk getbyunionid empty userid: body={}", response.body());
                throw new BusinessException(403, "非组织成员");
            }
            return userId;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("dingtalk getbyunionid exception", e);
            throw new BusinessException(401, "扫码登录失败");
        }
    }

    @Override
    public DingTalkUserProfile getUserProfile(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("userid", userId);
            HttpRequest request = HttpRequest.newBuilder(URI.create(GET_USER_URL + "?access_token=" + getAccessToken()))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());
            if (response.statusCode() != 200 || json.path("errcode").asInt(0) != 0) {
                log.warn("dingtalk user/get failed: status={} body={}", response.statusCode(), response.body());
                return null;
            }
            JsonNode result = json.path("result");
            DingTalkUserProfile profile = new DingTalkUserProfile();
            profile.setUserId(result.path("userid").asText(userId));
            profile.setName(result.path("name").asText(""));
            profile.setAvatar(result.path("avatar").asText(""));
            profile.setMobile(result.path("mobile").asText(""));
            return profile;
        } catch (Exception e) {
            log.warn("dingtalk user/get exception userId={}", userId, e);
            return null;
        }
    }
}
