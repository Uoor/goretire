package com.aliren.auth;

import com.aliren.common.BusinessException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/** 开发环境桩实现：真实换号在 Task 6 接入（DingTalkClientImpl） */
@Component
@ConditionalOnMissingBean(DingTalkClient.class)
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
