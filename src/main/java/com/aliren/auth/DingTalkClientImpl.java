package com.aliren.auth;

import com.aliren.common.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 钉钉免登换号（生产实现，基于钉钉开放平台 REST API + JDK HttpClient）：
 * 1) GET https://oapi.dingtalk.com/gettoken?appkey=&appsecret= 换 access_token（已实现）
 * 2) authCode -> unionId（H5 免登接口，需真实凭证后按开放平台文档补全）
 * 3) unionId -> userid（企业内部应用需同一组织，需补全）
 * 参考: https://open.dingtalk.com/document/orgapp/logon-free-process
 *
 * 条件装配：仅当配置了 aliren.dingtalk.app-key 时启用（与开发桩 DingTalkClientStub 互斥）。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "aliren.dingtalk", name = "app-key")
public class DingTalkClientImpl implements DingTalkClient {

    private static final String GET_TOKEN_URL = "https://oapi.dingtalk.com/gettoken";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final String appKey;
    private final String appSecret;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DingTalkClientImpl(@Value("${aliren.dingtalk.app-key}") String appKey,
                              @Value("${aliren.dingtalk.app-secret}") String appSecret) {
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    }

    @Override
    public String getUserIdByCode(String authCode) {
        String accessToken = getAccessToken();
        // TODO(Task 6 完成点): 按钉钉开放平台文档实现 authCode -> unionId -> userid
        // 需要真实 DING_APP_KEY / DING_APP_SECRET 与钉钉组织环境才能联调补全
        log.warn("dingtalk code exchange not yet implemented, got accessToken length={}, authCode length={}",
                accessToken.length(), authCode == null ? 0 : authCode.length());
        throw new BusinessException(500, "钉钉换号待接入：需配置 DING_APP_KEY / DING_APP_SECRET 并按开放平台文档实现");
    }

    private String getAccessToken() {
        try {
            URI uri = URI.create(GET_TOKEN_URL + "?appkey=" + appKey + "&appsecret=" + appSecret);
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(TIMEOUT).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());
            int errCode = json.path("errcode").asInt(-1);
            if (errCode != 0) {
                throw new BusinessException(500, "获取钉钉token失败: errcode=" + errCode
                        + " errmsg=" + json.path("errmsg").asText(""));
            }
            String token = json.path("access_token").asText("");
            if (token.isEmpty()) {
                throw new BusinessException(500, "获取钉钉token失败: access_token 为空");
            }
            return token;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("dingtalk gettoken failed", e);
            throw new BusinessException(500, "获取钉钉token异常");
        }
    }
}
