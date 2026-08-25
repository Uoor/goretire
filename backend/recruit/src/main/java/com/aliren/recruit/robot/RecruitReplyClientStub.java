package com.aliren.recruit.robot;

import com.aliren.recruit.aitable.AitableClient;
import com.aliren.recruit.aitable.RecruitRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 缺省回复桩：未启用招聘 @ 回复时生效，仅记录日志模拟接收与查询，
 * 保证链路在无钉钉凭证环境下可跑通。
 */
@Slf4j
public class RecruitReplyClientStub implements RecruitReplyClient {

    private final AitableClient aitableClient;

    public RecruitReplyClientStub(AitableClient aitableClient) {
        this.aitableClient = aitableClient;
    }

    @Override
    public void start() {
        log.info("[recruit-reply-stub] 启动群里 @ 招聘回复接收（模拟）");
    }

    @Override
    public void stop() {
        log.info("[recruit-reply-stub] 停止群里 @ 招聘回复接收（模拟）");
    }

    @Override
    public void handleGroupMention(GroupMentionMessage message) {
        log.info("[recruit-reply-stub] 收到群里 @ 招聘消息 {} -> {}: {}", message.senderNick(), message.msgtype(), message.text());
        List<RecruitRecord> found = aitableClient.query(message.text());
        log.info("[recruit-reply-stub] 命中 {} 条岗位", found.size());
    }

    @Override
    public List<RecruitRecord> searchRecruit(String keyword) {
        return aitableClient.query(keyword);
    }
}

@Configuration
class RecruitReplyClientConfig {

    @Bean
    @ConditionalOnMissingBean(RecruitReplyClient.class)
    RecruitReplyClient recruitReplyClientStub(AitableClient aitableClient) {
        return new RecruitReplyClientStub(aitableClient);
    }
}
