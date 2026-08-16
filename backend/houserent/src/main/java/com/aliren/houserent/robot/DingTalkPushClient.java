package com.aliren.houserent.robot;

import com.aliren.core.common.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
 * 钉钉推送真实实现：配置 aliren.robot.enabled=true 后启用。
 * 1) sendGroupCard：群自定义机器人 Webhook 发送 markdown 卡片（真实可用）
 * 2) sendWorkNotice：工作通知（私聊可达），需应用 agentId（aliren.robot.agent-id）；
 *    未配置时降级为日志（避免静默失败）。
 * 参考: https://open.dingtalk.com/document/orgapp/custom-robots-send-group-messages
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "aliren.robot", name = "enabled", havingValue = "true")
public class DingTalkPushClient implements PushClient {

    private static final String WORK_NOTICE_URL = "https://oapi.dingtalk.com/topapi/message/corpconversation/asyncsend_v2";
    private static final String GET_TOKEN_URL = "https://oapi.dingtalk.com/gettoken";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final String robotWebhook;
    private final String agentId;
    private final String appKey;
    private final String appSecret;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DingTalkPushClient(@Value("${aliren.robot.webhook:}") String robotWebhook,
                              @Value("${aliren.robot.agent-id:}") String agentId,
                              @Value("${aliren.dingtalk.app-key:}") String appKey,
                              @Value("${aliren.dingtalk.app-secret:}") String appSecret) {
        this.robotWebhook = robotWebhook;
        this.agentId = agentId;
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    }

    @Override
    public void sendGroupCard(String title, String markdown) {
        if (robotWebhook == null || robotWebhook.isBlank()) {
            log.warn("[dingtalk-push] webhook 未配置，跳过群推送: {}", title);
            return;
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("msgtype", "markdown");
            ObjectNode md = body.putObject("markdown");
            md.put("title", title);
            md.put("text", markdown);
            send(body);
        } catch (Exception e) {
            log.warn("[dingtalk-push] 群推送异常", e);
        }
    }

    /** actionCard：带跳转按钮的卡片（如新上架"查看详情"）；actionUrl 为空退化纯 markdown */
    @Override
    public void sendGroupCardAction(String title, String markdown, String actionUrl) {
        if (actionUrl == null || actionUrl.isBlank()) {
            sendGroupCard(title, markdown);
            return;
        }
        if (robotWebhook == null || robotWebhook.isBlank()) {
            log.warn("[dingtalk-push] webhook 未配置，跳过群推送: {}", title);
            return;
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("msgtype", "actionCard");
            ObjectNode card = body.putObject("actionCard");
            card.put("title", title);
            card.put("text", markdown);
            card.put("btnOrientation", "1"); // 按钮竖向排列
            ArrayNode btns = card.putArray("btns");
            ObjectNode btn = btns.addObject();
            btn.put("title", "查看详情");
            btn.put("actionURL", actionUrl);
            send(body);
        } catch (Exception e) {
            log.warn("[dingtalk-push] actionCard 推送异常", e);
        }
    }

    private void send(ObjectNode body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(robotWebhook))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode json = objectMapper.readTree(response.body());
        int errCode = json.path("errcode").asInt(-1);
        if (errCode != 0) {
            log.warn("[dingtalk-push] 群推送失败: errcode={} errmsg={}", errCode, json.path("errmsg").asText(""));
        } else {
            String _t = body.path("markdown").path("title").asText("");
            if (_t.isEmpty()) _t = body.path("actionCard").path("title").asText("");
            log.info("[dingtalk-push] 群推送成功: {}", _t);
        }
    }

    @Override
    public void sendWorkNotice(String userId, String markdown) {
        if (agentId == null || agentId.isBlank()) {
            log.warn("[dingtalk-push] agentId 未配置，工作通知降级为日志: {} -> {}", userId, markdown);
            return;
        }
        try {
            String accessToken = getAccessToken();
            ObjectNode body = objectMapper.createObjectNode();
            body.put("agent_id", Long.parseLong(agentId));
            body.put("userid_list", userId);
            ObjectNode msg = body.putObject("msg");
            msg.put("msgtype", "markdown");
            ObjectNode md = msg.putObject("markdown");
            md.put("title", "校友安居");
            md.put("text", markdown);
            HttpRequest request = HttpRequest.newBuilder(
                            URI.create(WORK_NOTICE_URL + "?access_token=" + accessToken))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());
            if (json.path("errcode").asInt(-1) != 0) {
                log.warn("[dingtalk-push] 工作通知失败: {} {}", json.path("errcode"), json.path("errmsg").asText(""));
            } else {
                log.info("[dingtalk-push] 工作通知成功 -> {}", userId);
            }
        } catch (Exception e) {
            log.warn("[dingtalk-push] 工作通知异常", e);
        }
    }

    private String getAccessToken() {
        try {
            URI uri = URI.create(GET_TOKEN_URL + "?appkey=" + appKey + "&appsecret=" + appSecret);
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(TIMEOUT).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());
            if (json.path("errcode").asInt(-1) != 0) {
                throw new BusinessException(500, "获取钉钉token失败");
            }
            return json.path("access_token").asText();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[dingtalk-push] gettoken 异常", e);
            throw new BusinessException(500, "获取钉钉token异常");
        }
    }
}
