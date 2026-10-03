package com.aliren.recruit.robot;

import com.aliren.recruit.aitable.AitableClient;
import com.aliren.recruit.aitable.RecruitRecord;

import java.util.List;

/**
 * 招聘模块 · 群里 @ 机器人后的接收与回复（双向对话）入口。
 * 职责：启动接收 → 把 @ 消息交给 {@link AitableClient} 查多维表 → 拼回复发回群里。
 *
 * 实现：
 * <ul>
 *   <li>StreamRecruitReplyClient —— Stream 模式长连接接收（配置 aliren.recruit.enabled 后启用）</li>
 *   <li>RecruitReplyClientStub —— 缺省桩，日志模拟，保证无凭证环境可跑通</li>
 * </ul>
 */
public interface RecruitReplyClient {

    /** 启动消息接收（Stream 长连接 / 回调注册）。应幂等。 */
    void start();

    /** 停止消息接收，释放连接。 */
    void stop();

    /** 处理群内 @ 消息：查多维表 {@link #searchRecruit} 后由实现决定如何回复。 */
    void handleGroupMention(GroupMentionMessage message);

    /** 按关键词实时查多维表（供实现与外部复用）。 */
    List<RecruitRecord> searchRecruit(String keyword);
}
