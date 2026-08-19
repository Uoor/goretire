package com.aliren.core.auth;

import com.aliren.core.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
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

    private final boolean devCodeEnabled;

    public DingTalkClientStub(boolean devCodeEnabled) {
        this.devCodeEnabled = devCodeEnabled;
    }

    @Override
    public String getUserIdByCode(String authCode) {
        if (authCode == null || authCode.isBlank()) {
            throw new BusinessException(401, "免登失败");
        }
        // 开发桩：仅当开关开启时放行 dev-code（生产 DEV_CODE_ENABLED=false 后彻底关闭）
        if (devCodeEnabled && "dev-code".equals(authCode)) {
            return authCode;
        }
        throw new BusinessException(401, "免登失败");
    }

    @Override
    public String getUserIdByOAuthCode(String authCode) {
        // 开发桩：不支持 OAuth2 扫码登录
        throw new BusinessException(401, "扫码登录失败");
    }

    @Override
    public DingTalkUserProfile getUserProfile(String userId) {
        // 开发桩：无真实钉钉资料，返回默认占位
        if (devCodeEnabled && "dev-code".equals(userId)) {
            DingTalkUserProfile profile = new DingTalkUserProfile();
            profile.setUserId(userId);
            profile.setName("校友");
            return profile;
        }
        return null;
    }
}

@Configuration
class DingTalkClientConfig {

    @Bean
    @ConditionalOnMissingBean(DingTalkClient.class)
    DingTalkClient dingTalkClientStub(@Value("${aliren.auth.dev-code-enabled:true}") boolean devCodeEnabled) {
        return new DingTalkClientStub(devCodeEnabled);
    }
}
