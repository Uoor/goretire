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

    /** 钉钉互动卡片（模板卡片）发送接口 */
    private static final String SEND_URL = "https://api.dingtalk.com/v1.0/im/interactiveCards/send";
    private static final Duration TIMEOUT = Duration.ofSeconds(8);
    /** 岗位描述摘要截断长度（对齐 sample DIGEST_PER_JOB） */
    private static final int DIGEST_PER_JOB = 120;

    private final String appKey;
    private final String appSecret;
    private final String robotCode;
    private final String cardTemplateId;
    private final int maxResults;
    private final String noResultTip;
    private final String baseId;
    private final String tableId;
    private final String viewId;
    private final String jobsListUrl;
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
                                    @Value("${aliren.recruit.reply.card-template-id:}") String cardTemplateId,
                                    @Value("${aliren.recruit.reply.jobs-list-url:}") String jobsListUrl,
                                    @Value("${aliren.recruit.aitable.base-id:}") String baseId,
                                    @Value("${aliren.recruit.aitable.table-id:}") String tableId,
                                    @Value("${aliren.recruit.aitable.view-id:UuvnFan}") String viewId,
                                    AitableClient aitableClient,
                                    DingTalkTokenClient tokenClient) {
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.robotCode = robotCode;
        this.cardTemplateId = cardTemplateId;
        this.maxResults = maxResults <= 0 ? 5 : maxResults;
        this.noResultTip = noResultTip;
        this.baseId = baseId;
        this.tableId = tableId;
        this.viewId = viewId;
        // "查看全部岗位"（detailUrl）：优先用配置的岗位列表分享链接，否则回退多维表视图
        this.jobsListUrl = jobsListUrl == null || jobsListUrl.isBlank()
                ? (baseId == null || baseId.isBlank() || tableId == null || tableId.isBlank()
                    ? "https://alidocs.dingtalk.com"
                    : "https://alidocs.dingtalk.com/i/nodes/" + baseId + "?entrance=data&sheetId=" + tableId)
                : jobsListUrl;
        this.aitableClient = aitableClient;
        this.tokenClient = tokenClient;
    }

    /**
     * 岗位详情链接（sample 验证可用格式）：
     * https://alidocs.dingtalk.com/i/nodes/{baseId}?iframeQuery=record%3D{sheetId}_{viewId}_{rowId}&notable_standalone_record_redirect=true
     */
    private String recordUrl(RecruitRecord r) {
        if (baseId == null || baseId.isBlank() || tableId == null || tableId.isBlank()
                || r.recordId() == null || r.recordId().isBlank()) {
            return jobsListUrl;
        }
        return "https://alidocs.dingtalk.com/i/nodes/" + baseId
                + "?iframeQuery=record%3D" + tableId + "_" + viewId + "_" + r.recordId()
                + "&notable_standalone_record_redirect=true";
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
            java.util.Map<String, String> paramMap;
            int count;
            if (text.isEmpty()) {
                // 空 @ → 过去一周新增岗位总结；无新增时回退展示按时间最近的 maxResults 条
                List<RecruitRecord> recent = aitableClient.queryRecent(WEEK_DAYS);
                boolean fallback = recent.isEmpty();
                if (fallback) {
                    recent = aitableClient.queryLatest(maxResults);
                }
                count = recent.size();
                paramMap = buildWeeklyCard(recent, fallback);
            } else {
                List<RecruitRecord> found = searchRecruit(text);
                count = found.size();
                paramMap = buildSearchCard(found, text);
            }
            sendInteractiveCard(message.openConversationId(), paramMap);
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

    // ---- 互动卡片（模板卡片，对齐 sample）----

    /** 关键词搜索卡片（新模板 title 支持 $title 变量，标题直接展示搜索标识） */
    private java.util.Map<String, String> buildSearchCard(List<RecruitRecord> found, String keyword) {
        java.util.Map<String, String> m = new java.util.LinkedHashMap<>();
        m.put("title", found.isEmpty() ? "🔍 未找到匹配岗位" : "🔍 找到 " + found.size() + " 个岗位（" + keyword + "）");
        m.put("tag", "招聘");
        m.put("tagColor", "blue");
        m.put("jobTitle", found.isEmpty() ? "换个关键词试试" : "点击查看详情");
        m.put("company", "");
        m.put("location", "");
        m.put("contact", "");
        m.put("salary", "");
        m.put("tag1", "");
        m.put("tag2", "");
        m.put("tag3", "");
        m.put("descriptionMd", buildDescriptionMd(found));
        m.put("requirementMd", "");
        m.put("detailUrl", jobsListUrl);
        m.put("contactUrl", "");
        return m;
    }

    /** 空 @ 周报卡片（fallback=无新增展示最近岗位） */
    private java.util.Map<String, String> buildWeeklyCard(List<RecruitRecord> recent, boolean fallback) {
        java.util.Map<String, String> m = new java.util.LinkedHashMap<>();
        m.put("title", recent.isEmpty() ? "📢 本周热招" : "📢 本周热招 · " + recent.size() + "个岗位");
        m.put("tag", "招聘");
        m.put("tagColor", "blue");
        m.put("jobTitle", fallback ? "过去一周暂无新增，以下为最近岗位" : "点击详情按钮查看全部岗位");
        m.put("company", "");
        m.put("location", "");
        m.put("contact", "");
        m.put("salary", "");
        m.put("tag1", "");
        m.put("tag2", "");
        m.put("tag3", "");
        m.put("descriptionMd", buildDescriptionMd(recent));
        m.put("requirementMd", "");
        m.put("detailUrl", jobsListUrl);
        m.put("contactUrl", "");
        return m;
    }

    /** 岗位列表 markdown：📌 **职位名** · 公司 \n 摘要 \n 👉 [查看详情](记录链接) */
    private String buildDescriptionMd(List<RecruitRecord> records) {
        if (records.isEmpty()) {
            return noResultTip == null || noResultTip.isBlank()
                    ? "暂时没找到匹配岗位，换个关键词试试。"
                    : noResultTip;
        }
        List<String> blocks = new java.util.ArrayList<>();
        records.stream().limit(maxResults).forEach(r -> blocks.add(jobBlockMd(r)));
        if (records.size() > maxResults) {
            blocks.add("> 仅展示前 " + maxResults + " 条，共 " + records.size() + " 条");
        }
        return String.join("\n\n", blocks);
    }

    /** 单条岗位 markdown（对齐 sample：📌 加粗名称 · 公司 + 摘要 + 查看详情链接） */
    private String jobBlockMd(RecruitRecord r) {
        StringBuilder sb = new StringBuilder();
        String name = r.title() == null || r.title().isBlank() ? "（未命名岗位）" : r.title();
        sb.append("📌 **").append(name).append("**");
        if (r.company() != null && !r.company().isBlank()) sb.append(" · ").append(r.company());
        String digest = shorten(r.description(), DIGEST_PER_JOB);
        if (digest != null && !digest.isBlank()) {
            sb.append("  \n").append(digest);
        }
        sb.append("  \n👉 [查看详情](").append(recordUrl(r)).append(")");
        return sb.toString();
    }

    /** 摘要截断：压平换行 + 超长截断（对齐 sample shorten） */
    private String shorten(String text, int maxChars) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String flat = String.join(" ", text.strip().split("\\r?\\n")).trim();
        if (flat.length() > maxChars) {
            return flat.substring(0, maxChars).stripTrailing() + " ...";
        }
        return flat;
    }

    /** 发送钉钉互动卡片（模板卡片） */
    private void sendInteractiveCard(String openConversationId, java.util.Map<String, String> paramMap) {
        if (cardTemplateId == null || cardTemplateId.isBlank()) {
            log.warn("[recruit-stream] card-template-id 未配置，跳过卡片发送");
            return;
        }
        try {
            String token = tokenClient.getAccessToken(appKey, appSecret);
            ObjectNode cardData = objectMapper.createObjectNode();
            ObjectNode cardParam = cardData.putObject("cardParamMap");
            paramMap.forEach(cardParam::put);

            ObjectNode body = objectMapper.createObjectNode();
            body.put("cardTemplateId", cardTemplateId);
            body.put("openConversationId", openConversationId);
            body.put("conversationType", 1);
            body.put("robotCode", robotCode);
            body.put("outTrackId", "recruit-reply-" + System.currentTimeMillis());
            body.set("cardData", cardData);
            body.put("userIdType", 1);

            HttpRequest request = HttpRequest.newBuilder(URI.create(SEND_URL))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("x-acs-dingtalk-access-token", token)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String respBody = response.body();
            if (response.statusCode() >= 400) {
                log.warn("[recruit-stream] 卡片发送失败: HTTP {} {}", response.statusCode(), respBody);
            } else {
                log.info("[recruit-stream] 卡片发送成功: {}", respBody);
            }
        } catch (BusinessException e) {
            log.warn("[recruit-stream] 卡片发送失败: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("[recruit-stream] 卡片发送异常", e);
        }
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}
