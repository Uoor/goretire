package com.aliren.houserent.robot;

/**
 * 消息推送抽象（机器人触达）。
 * 实现：DingTalkPushClient（配置 aliren.robot.enabled 后启用，接钉钉机器人/工作通知）；
 * PushClientStub（缺省，日志输出模拟发送，便于无凭证联调与测试）。
 *
 * 推送场景（见产品文档 §7）：
 * - 审核通过：新上架卡片 → 子群（sendGroupCard）
 * - 每周五：精选周推 → 子群（sendGroupCard）
 * - 每周一：安居故事 → 子群（sendGroupCard）
 * - 订阅/求租匹配：私聊提醒 → 工作通知（sendWorkNotice）
 */
public interface PushClient {

    /** 推送富文本卡片到群（新上架/周推/故事） */
    void sendGroupCard(String title, String markdown);

    /** 推送工作通知（私聊触达，订阅/求租匹配提醒） */
    void sendWorkNotice(String userId, String markdown);
}
