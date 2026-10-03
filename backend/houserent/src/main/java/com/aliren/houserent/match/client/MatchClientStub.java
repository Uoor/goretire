package com.aliren.houserent.match.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 缺省桩：未配置 LLM API Key 时启用，complete() 恒返回 null，
 * 由 MatchService 走本地简单过滤降级（保证匹配主流程不因 LLM 不可用而中断）。
 */
@Slf4j
public class MatchClientStub implements MatchClient {

    @Override
    public String complete(String prompt) {
        log.debug("llm not configured, fallback to local filter");
        return null;
    }
}

@Configuration
class MatchClientConfig {

    @Bean
    @ConditionalOnMissingBean(MatchClient.class)
    MatchClient matchClientStub() {
        return new MatchClientStub();
    }
}
