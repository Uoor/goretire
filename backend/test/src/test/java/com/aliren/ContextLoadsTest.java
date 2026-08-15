package com.aliren;

import com.aliren.auth.DingTalkClient;
import com.aliren.auth.DingTalkClientStub;
import com.aliren.user.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** 验证 Spring 上下文可启动：UserMapper 被注册为 Mapper，且 DingTalkClient 注入的是开发桩。 */
@SpringBootTest
class ContextLoadsTest {

    @Autowired
    private DingTalkClient dingTalkClient;
    @Autowired
    private UserMapper userMapper;

    @Test
    void contextLoads() {
        assertThat(userMapper).isNotNull();
        assertThat(dingTalkClient).isInstanceOf(DingTalkClientStub.class);
    }
}
