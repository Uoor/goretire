package com.aliren.recruit.robot;

import com.aliren.core.common.BusinessException;
import com.aliren.recruit.aitable.AitableClient;
import com.aliren.recruit.aitable.RecruitRecord;
import com.aliren.recruit.dingtalk.DingTalkTokenClient;
import com.dingtalk.open.app.api.OpenDingTalkClient;
import com.dingtalk.open.app.api.OpenDingTalkStreamClientBuilder;
import com.dingtalk.open.app.api.callback.DingTalkStreamTopics;
import com.dingtalk.open.app.api.message.GenericOpenDingTalkEvent;
import com.dingtalk.open.app.api.security.AuthClientCredential;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 招聘模块 · Stream 接收 + 回复（真实实现）。
 * 通过 DingStream 长连接订阅钉钉群机器人 @ 消息（主题 /v1.0/im/bot/messages/get），
 * 收到后查多维表并调用 /v1.0/robot/groupMessages/send 回复到群里。
 *
 * 依赖：应用需开通机器人回调（Stream）权限，且对应应用已开通多维表 {@code Notable.Base.Read}。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "aliren.recruit", name = "enabled", havingValue = "true")
public class StreamRecruitReplyClient implements RecruitReplyClient, SmartLifecycle {

    private static final String SEND_URL = "https://api.dingtalk.com/v1.0/robot/groupMessages/send";
    private static final Duration TIMEOUT = Duration.ofSeconds(8);

    private final String appKey;
    private final String appSecret;
    private final String robotCode;
    private final int maxResults;
    private final String noResultTip;
    private final AitableClient aitableClient;
    private final DingTalkTokenClient tokenClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    private volatile OpenDingTalkClient streamClient;

    public StreamRecruitReplyClient(@Value("${aliren.recruit.robot.app-key:}") String appKey,
                                    @Value("${aliren.recruit.robot.app-secret:}") String appSecret,
                                    @Value("${aliren.recruit.robot.robot-code:}") String robotCode,
                                    @Value("${aliren.recruit.reply.max-results:5}") int maxResults,
                                    @Value("${aliren.recruit.reply.no-result-tip:}") String noResultTip,
                                    AitableClient aitableClient,
                                    DingTalkTokenClient tokenClient) {
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.robotCode = robotCode;
        this.maxResults = maxResults <= 0 ? 5 : maxResults;
        this.noResultTip = noResultTip;
        this.aitableClient = aitableClient;
        this.tokenClient = tokenClient;
    }

    @Override
    public void start() {
        if (appKey == null || appKey.isBlank() || appSecret == null || appSecret.isBlank()) {
            log.warn("[recruit-stream] 机器人应用凭证未配置，跳过启动 Stream");
            return;
        }
        try {
            streamClient = OpenDingTalkStreamClientBuilder.custom()
                    .credential(new AuthClientCredential(appKey, appSecret))
                    // 诊断：接收全部事件并打日志，确认 @ 消息是否到达（上线稳定后可移除）
                    .registerAllEventListener(event -> {
                        Object data = event.getData();
                        String dataStr = data == null ? "null" : String.valueOf(data);
                        if (dataStr.length() > 300) dataStr = dataStr.substring(0, 300);
                        log.info("[recruit-stream] 收到事件: type={} id={} corpId={} data={}",
                                event.getEventType(), event.getEventId(), event.getEventCorpId(), dataStr);
                        return com.dingtalk.open.app.stream.protocol.event.EventAckStatus.SUCCESS;
                    })
                    .registerCallbackListener(DingTalkStreamTopics.BOT_MESSAGE_TOPIC,
                            (GenericOpenDingTalkEvent req) -> {
                                onStreamEvent(req);
                                return null;
                            })
                    .build();
            streamClient.start();
            log.info("[recruit-stream] DingStream 已启动，订阅机器人 @ 消息");
        } catch (Exception e) {
            log.warn("[recruit-stream] 启动 Stream 失败", e);
        }
    }

    @Override
    public void stop() {
        OpenDingTalkClient client = streamClient;
        streamClient = null;
        if (client != null) {
            try {
                client.stop();
                log.info("[recruit-stream] DingStream 已停止");
            } catch (Exception e) {
                log.warn("[recruit-stream] 停止 Stream 异常", e);
            }
        }
    }

    @Override
    public boolean isRunning() {
        return streamClient != null;
    }

    @Override
    public void handleGroupMention(GroupMentionMessage message) {
        if (message == null || !message.replyable()) {
            return;
        }
        try {
            List<RecruitRecord> found = searchRecruit(message.text());
            String reply = formatReply(found, message.senderNick());
            sendGroupReply(message.openConversationId(), reply);
            log.info("[recruit-stream] 已回复 @{}: 命中 {} 条", message.senderNick(), found.size());
        } catch (Exception e) {
            log.warn("[recruit-stream] 处理 @ 消息异常", e);
        }
    }

    @Override
    public List<RecruitRecord> searchRecruit(String keyword) {
        return aitableClient.query(keyword);
    }

    // ---- 内部 ----

    private void onStreamEvent(GenericOpenDingTalkEvent event) {
        try {
            Object data = event.getData();
            if (data == null) {
                return;
            }
            GroupMentionMessage message = parseMention(event);
            if (message != null) {
                handleGroupMention(message);
            }
        } catch (Exception e) {
            log.warn("[recruit-stream] 解析 @ 消息异常", e);
        }
    }

    @SuppressWarnings("unchecked")
    private GroupMentionMessage parseMention(GenericOpenDingTalkEvent event) {
        shade.com.alibaba.fastjson2.JSONObject data = (shade.com.alibaba.fastjson2.JSONObject) event.getData();
        String openConversationId = firstNonBlank(data.getString("openConversationId"), data.getString("conversationId"));
        String text = null;
        Object textObj = data.get("text");
        if (textObj instanceof String s) {
            text = s;
        } else if (textObj instanceof shade.com.alibaba.fastjson2.JSONObject o) {
            text = o.getString("content");
        }
        return new GroupMentionMessage(
                openConversationId,
                firstNonBlank(data.getString("robotCode"), robotCode),
                data.getString("senderStaffId"),
                data.getString("senderNick"),
                text,
                data.getString("msgId"),
                data.getString("msgtype")
        );
    }

    private String formatReply(List<RecruitRecord> found, String senderNick) {
        StringBuilder sb = new StringBuilder();
        if (senderNick != null && !senderNick.isBlank()) {
            sb.append("@" + senderNick).append("\n");
        }
        if (found.isEmpty()) {
            sb.append(noResultTip == null || noResultTip.isBlank()
                    ? "暂时没找到匹配岗位，换个关键词试试。"
                    : noResultTip);
            return sb.toString();
        }
        sb.append("找到 ").append(found.size()).append(" 个岗位：\n\n");
        found.stream().limit(maxResults).forEach(r -> {
            sb.append("▶ ").append(r.title()).append("\n");
            if (r.company() != null && !r.company().isBlank()) sb.append("· ").append(r.company());
            if (!r.locations().isEmpty()) sb.append("  [").append(String.join("/", r.locations())).append("]");
            if (r.salary() != null && !r.salary().isBlank()) sb.append("\n· 薪资：").append(r.salary());
            if (r.priority() != null && !r.priority().isBlank()) sb.append("\n· 标签：").append(r.priority());
            sb.append("\n\n");
        });
        if (found.size() > maxResults) {
            sb.append("（仅展示前 ").append(maxResults).append(" 条，私聊招聘同学了解更多）");
        }
        return sb.toString();
    }

    private void sendGroupReply(String openConversationId, String content) {
        try {
            String token = tokenClient.getAccessToken(appKey, appSecret);
            ObjectNode body = objectMapper.createObjectNode();
            body.put("robotCode", robotCode);
            body.put("openConversationId", openConversationId);
            body.put("msgKey", "sampleText");
            body.put("msgParam", objectMapper.createObjectNode().put("content", content).toString());

            HttpRequest request = HttpRequest.newBuilder(URI.create(SEND_URL))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("x-acs-dingtalk-access-token", token)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String respBody = response.body();
            if (response.statusCode() >= 400) {
                log.warn("[recruit-stream] 群回复失败: HTTP {} {}", response.statusCode(), respBody);
            } else {
                log.info("[recruit-stream] 群回复成功: {}", respBody);
            }
        } catch (BusinessException e) {
            log.warn("[recruit-stream] 群回复失败: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("[recruit-stream] 群回复异常", e);
        }
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}
