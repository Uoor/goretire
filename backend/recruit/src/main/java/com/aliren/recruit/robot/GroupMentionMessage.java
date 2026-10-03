package com.aliren.recruit.robot;

/**
 * 群里 @ 招聘机器人产生的入站消息。
 * 字段对应钉钉群机器人 Stream（RobotCallback 主题）回调事件的核心 payload。
 *
 * @param openConversationId 群开放会话 ID，回复时的目标群
 * @param robotCode          机器人编码
 * @param senderStaffId      提问人 userId（用于 @ 回对方）
 * @param senderNick         提问人昵称
 * @param text               用户 @ 后输入的内容
 * @param msgId              消息 ID（用于去重）
 * @param msgtype            消息类型（text 等）
 */
public record GroupMentionMessage(
        String openConversationId,
        String robotCode,
        String senderStaffId,
        String senderNick,
        String text,
        String msgId,
        String msgtype
) {

    /** 是否为可回复的群聊消息（有群开放会话 ID）。 */
    public boolean replyable() {
        return openConversationId != null && !openConversationId.isBlank();
    }
}
