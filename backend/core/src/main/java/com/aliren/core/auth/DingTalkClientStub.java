package com.aliren.core.auth;

import com.aliren.core.common.BusinessException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 开发环境桩实现：真实换号在配置 DING_APP_KEY 后由 DingTalkClientImpl 接管。
 *
 * 注意：@ConditionalOnMissingBean 不能直接放在 @Component 类上（Spring Boot 已知陷阱）——
 * 组件扫描会先注册自身的 bean 定义，条件评估时 getBeanNamesForType 会匹配到自身，
 * 导致桩永远不生效。因此条件放在 @Bean 方法上声明。
 */
public class DingTalkClientStub implements DingTalkClient {
    @Override
    public String getUserIdByCode(String authCode) {
        if (authCode == null || authCode.isBlank()) {
            throw new BusinessException(401, "免登失败");
        }
        // 开发桩：authCode 即视为钉钉 userid（便于本地联调）
        return authCode;
    }
}

@Configuration
class DingTalkClientConfig {

    @Bean
    @ConditionalOnMissingBean(DingTalkClient.class)
    DingTalkClient dingTalkClientStub() {
        return new DingTalkClientStub();
    }
}
