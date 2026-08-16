package com.aliren.houserent.robot;

/**
 * 消息推送抽象（机器人触达）。
 * 实现：DingTalkPushClient（配置 aliren.robot.enabled 后启用，接钉钉机器人/工作通知）；
 * PushClientStub（缺省，日志输出模拟发送，便于无凭证联调与测试）。
 *
 * 推送场景（见产品文档 §7）：
 * - 审核通过：新上架卡片（带"查看详情"按钮）→ 子群（sendGroupCardAction）
 * - 每周五：精选周推 → 子群（sendGroupCard）
 * - 每周一：安居故事 → 子群（sendGroupCard）
 * - 订阅/求租匹配：私聊提醒 → 工作通知（sendWorkNotice）
 */
public interface PushClient {

    /** 推送富文本卡片到群（周推/故事等，无需跳转） */
    void sendGroupCard(String title, String markdown);

    /** 推送带跳转按钮的群卡片（actionCard，如新上架"查看详情"）；actionUrl 为空时退化为 sendGroupCard */
    default void sendGroupCardAction(String title, String markdown, String actionUrl) {
        sendGroupCard(title, markdown);
    }

    /** 推送工作通知（私聊触达，订阅/求租匹配提醒） */
    void sendWorkNotice(String userId, String markdown);
}
