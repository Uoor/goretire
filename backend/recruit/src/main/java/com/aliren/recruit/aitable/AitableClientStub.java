package com.aliren.recruit.aitable;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 缺省多维表桩：未配置凭证时生效，仅记录日志模拟查询，
 * 保证招聘链路在无钉钉凭证环境下可跑通（与 PushClientStub 同模式）。
 */
@Slf4j
public class AitableClientStub implements AitableClient {

    @Override
    public List<RecruitRecord> query(String keyword) {
        log.info("[recruit-aitable-stub] 模拟查询多维表: keyword={}", keyword);
        return List.of();
    }

    @Override
    public List<RecruitRecord> queryRecent(int days) {
        log.info("[recruit-aitable-stub] 模拟查询最近 {} 天岗位", days);
        return List.of();
    }
}

@Configuration
class AitableClientConfig {

    @Bean
    @ConditionalOnMissingBean(AitableClient.class)
    AitableClient aitableClientStub() {
        return new AitableClientStub();
    }
}
