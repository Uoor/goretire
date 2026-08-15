package com.aliren.houserent.robot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 钉钉推送真实实现（骨架）：配置 aliren.robot.enabled=true 且提供钉钉凭证后启用。
 *
 * TODO(凭证就绪): 按钉钉开放平台补全两类真实发送——
 *  1) 群卡片：企业内部机器人（Stream 模式长连接）或群自定义机器人 Webhook，发送交互卡片到「校友安居」子群；
 *  2) 工作通知：调用工作通知 API（按 userid 主动可达），作为订阅/求租匹配私聊推送的兜底；
 *  需 access_token（可复用 DingTalkClientImpl.getAccessToken 模式）与机器人 key/secret。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "aliren.robot", name = "enabled", havingValue = "true")
public class DingTalkPushClient implements PushClient {

    private final String robotWebhook;

    public DingTalkPushClient(@Value("${aliren.robot.webhook:}") String robotWebhook) {
        this.robotWebhook = robotWebhook;
    }

    @Override
    public void sendGroupCard(String title, String markdown) {
        log.warn("[dingtalk-push] 群卡片发送待接入：webhook={} title={}", robotWebhook, title);
        // TODO(凭证就绪): 组装交互卡片 JSON 并 POST 到群机器人 webhook / Stream 发送
    }

    @Override
    public void sendWorkNotice(String userId, String markdown) {
        log.warn("[dingtalk-push] 工作通知发送待接入：userId={}", userId);
        // TODO(凭证就绪): 调用钉钉工作通知 API（agentId + userid_list + markdown 消息体）
    }
}
