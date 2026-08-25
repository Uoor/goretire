package com.aliren.recruit.robot;

import com.aliren.core.common.BusinessException;
import com.aliren.recruit.aitable.AitableClient;
import com.aliren.recruit.aitable.RecruitRecord;
import com.aliren.recruit.dingtalk.DingTalkTokenClient;
import com.dingtalk.open.app.api.OpenDingTalkClient;
import com.dingtalk.open.app.api.OpenDingTalkStreamClientBuilder;
import com.dingtalk.open.app.api.callback.DingTalkStreamTopics;
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
    private final String allJobsUrl;
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
                                    @Value("${aliren.recruit.aitable.base-id:}") String baseId,
                                    @Value("${aliren.recruit.aitable.table-id:}") String tableId,
                                    AitableClient aitableClient,
                                    DingTalkTokenClient tokenClient) {
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.robotCode = robotCode;
        this.maxResults = maxResults <= 0 ? 5 : maxResults;
        this.noResultTip = noResultTip;
        // "查看全部岗位" 按钮与每条岗位链接 → 多维表视图
        this.allJobsUrl = baseId == null || baseId.isBlank() || tableId == null || tableId.isBlank()
                ? "https://alidocs.dingtalk.com"
                : "https://alidocs.dingtalk.com/i/nodes/" + baseId + "?entrance=data&sheetId=" + tableId;
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
                    // 官方示例：机器人回调请求体直接是消息 JSON（OpenDingTalkCallbackListener<JSONObject, JSONObject>），
                    // 不能用 GenericOpenDingTalkEvent（会反序列化错类型导致 getData() 为 null）
                    .registerCallbackListener(DingTalkStreamTopics.BOT_MESSAGE_TOPIC,
                            (shade.com.alibaba.fastjson2.JSONObject req) -> {
                                handleBotMessage(req);
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

    /** 空 @（无文字）时总结的天数 */
    private static final int WEEK_DAYS = 7;

    /** 消息去重：msgId → 到达时间，短窗口内不重复回复 */
    private static final long DEDUP_WINDOW_MS = 60_000L;
    private final java.util.Map<String, Long> recentMsgIds = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public void handleGroupMention(GroupMentionMessage message) {
        if (message == null || !message.replyable()) {
            return;
        }
        if (isDuplicate(message.msgId())) {
            log.info("[recruit-stream] 忽略重复消息 msgId={}", message.msgId());
            return;
        }
        try {
            String text = message.text() == null ? "" : message.text().trim();
            String title;
            String body;
            int count;
            if (text.isEmpty()) {
                // 空 @ → 过去一周新增岗位总结
                List<RecruitRecord> recent = aitableClient.queryRecent(WEEK_DAYS);
                count = recent.size();
                title = formatWeeklyTitle(recent);
                body = formatWeeklyBody(recent, message.senderNick());
            } else {
                List<RecruitRecord> found = searchRecruit(text);
                count = found.size();
                title = formatSearchTitle(found, text);
                body = formatSearchBody(found, message.senderNick());
            }
            sendGroupReply(message.openConversationId(), title, body);
            log.info("[recruit-stream] 已回复 @{}: {} 条", message.senderNick(), count);
        } catch (Exception e) {
            log.warn("[recruit-stream] 处理 @ 消息异常", e);
        }
    }

    /** 短窗口 msgId 去重 */
    private boolean isDuplicate(String msgId) {
        if (msgId == null || msgId.isBlank()) {
            return false;
        }
        long now = System.currentTimeMillis();
        Long prev = recentMsgIds.putIfAbsent(msgId, now);
        if (prev == null) {
            if (recentMsgIds.size() > 500) {
                recentMsgIds.entrySet().removeIf(e -> now - e.getValue() > DEDUP_WINDOW_MS);
            }
            return false;
        }
        return now - prev < DEDUP_WINDOW_MS;
    }

    @Override
    public List<RecruitRecord> searchRecruit(String keyword) {
        return aitableClient.query(keyword);
    }

    // ---- 内部 ----

    /** 机器人回调：请求体即消息 JSON（官方 RobotMsgCallbackConsumer 模式） */
    private void handleBotMessage(shade.com.alibaba.fastjson2.JSONObject req) {
        try {
            log.info("[recruit-stream] 收到机器人消息: {}", req);
            GroupMentionMessage message = parseMention(req);
            if (message != null) {
                handleGroupMention(message);
            }
        } catch (Exception e) {
            log.warn("[recruit-stream] 解析 @ 消息异常", e);
        }
    }

    private GroupMentionMessage parseMention(shade.com.alibaba.fastjson2.JSONObject data) {
        // conversationType: 1=单聊 2=群聊（官方文档）
        String conversationType = data.getString("conversationType");
        if (!"2".equals(conversationType)) {
            log.info("[recruit-stream] 忽略非群聊消息 conversationType={}", conversationType);
            return null;
        }
        // 群会话 ID（官方示例直接用 conversationId 作为回复目标 openConversationId）
        String openConversationId = data.getString("conversationId");
        String text = null;
        shade.com.alibaba.fastjson2.JSONObject textObj = data.getJSONObject("text");
        if (textObj != null) {
            text = textObj.getString("content");
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

    /** 关键词查询：卡片标题 */
    private String formatSearchTitle(List<RecruitRecord> found, String keyword) {
        if (found.isEmpty()) {
            return "🔍 招聘查询";
        }
        return "🔍 找到 " + found.size() + " 个岗位（" + keyword + "）";
    }

    /** 关键词查询：卡片正文（markdown，岗位带链接） */
    private String formatSearchBody(List<RecruitRecord> found, String senderNick) {
        StringBuilder sb = new StringBuilder();
        if (senderNick != null && !senderNick.isBlank()) {
            sb.append("**@").append(senderNick).append("**\n\n");
        }
        if (found.isEmpty()) {
            sb.append(noResultTip == null || noResultTip.isBlank()
                    ? "暂时没找到匹配岗位，换个关键词试试。"
                    : noResultTip);
            return sb.toString();
        }
        found.stream().limit(maxResults).forEach(r -> appendJobMd(sb, r));
        if (found.size() > maxResults) {
            sb.append("\n> 仅展示前 ").append(maxResults).append(" 条，共 ").append(found.size()).append(" 条");
        }
        return sb.toString();
    }

    /** 空 @ 周报：卡片标题 */
    private String formatWeeklyTitle(List<RecruitRecord> recent) {
        if (recent.isEmpty()) {
            return "📋 过去一周招聘岗位";
        }
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate weekAgo = today.minusDays(WEEK_DAYS - 1);
        return "📋 过去一周（" + weekAgo.getMonthValue() + "." + weekAgo.getDayOfMonth()
                + " - " + today.getMonthValue() + "." + today.getDayOfMonth()
                + "）新增 " + recent.size() + " 个岗位";
    }

    /** 空 @ 周报：卡片正文（markdown，岗位带链接，最新在前急聘优先） */
    private String formatWeeklyBody(List<RecruitRecord> recent, String senderNick) {
        StringBuilder sb = new StringBuilder();
        if (senderNick != null && !senderNick.isBlank()) {
            sb.append("**@").append(senderNick).append("**\n\n");
        }
        if (recent.isEmpty()) {
            sb.append("过去一周暂无新增岗位。");
            return sb.toString();
        }
        recent.stream().limit(maxResults).forEach(r -> appendJobMd(sb, r));
        if (recent.size() > maxResults) {
            sb.append("\n> 仅展示前 ").append(maxResults).append(" 条，共 ").append(recent.size()).append(" 条");
        }
        return sb.toString();
    }

    /** 单条岗位 markdown：标题带链接 + 公司/地点/薪资/标签 */
    private void appendJobMd(StringBuilder sb, RecruitRecord r) {
        String title = r.title() == null || r.title().isBlank() ? "（未命名岗位）" : r.title();
        sb.append("### [").append(title).append("](").append(allJobsUrl).append(")\n");
        sb.append("> ");
        if (r.company() != null && !r.company().isBlank()) sb.append(r.company());
        if (!r.locations().isEmpty()) sb.append(" · ").append(String.join("/", r.locations()));
        sb.append("\n");
        if (r.salary() != null && !r.salary().isBlank()) sb.append("> 薪资：").append(r.salary()).append("\n");
        if (r.priority() != null && !r.priority().isBlank()) sb.append("> ").append(r.priority()).append("\n");
        sb.append("\n");
    }

    /** 发送 ActionCard 卡片：markdown 正文 + 底部"查看全部岗位"按钮 */
    private void sendGroupReply(String openConversationId, String title, String body) {
        try {
            String token = tokenClient.getAccessToken(appKey, appSecret);
            ObjectNode card = objectMapper.createObjectNode();
            card.put("title", title);
            card.put("text", body);
            card.put("singleTitle", "查看全部岗位");
            card.put("singleURL", allJobsUrl);

            ObjectNode msg = objectMapper.createObjectNode();
            msg.put("robotCode", robotCode);
            msg.put("openConversationId", openConversationId);
            msg.put("msgKey", "sampleActionCard");
            msg.put("msgParam", card.toString());

            HttpRequest request = HttpRequest.newBuilder(URI.create(SEND_URL))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("x-acs-dingtalk-access-token", token)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(msg)))
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
