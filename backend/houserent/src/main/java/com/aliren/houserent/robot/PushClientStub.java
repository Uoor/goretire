package com.aliren.houserent.robot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 缺省推送桩：未启用机器人时生效，仅记录日志模拟发送，
 * 保证编排/定时任务在无钉钉凭证环境下可跑通全链路。
 */
@Slf4j
public class PushClientStub implements PushClient {

    @Override
    public void sendGroupCard(String title, String markdown) {
        log.info("[push-stub] 群卡片: {} \n{}", title, markdown);
    }

    @Override
    public void sendWorkNotice(String userId, String markdown) {
        log.info("[push-stub] 工作通知 -> {}: {}", userId, markdown);
    }
}

@Configuration
class PushClientConfig {

    @Bean
    @ConditionalOnMissingBean(PushClient.class)
    PushClient pushClientStub() {
        return new PushClientStub();
    }
}
