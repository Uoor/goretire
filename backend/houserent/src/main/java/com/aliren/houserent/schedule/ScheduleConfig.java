package com.aliren.houserent.schedule;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务开关：aliren.schedule.enabled（缺省 true）。
 * 周五精选周推 / 周一安居故事；真实推送依赖 robot 凭证，未配置时走 PushClientStub 日志。
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "aliren.schedule", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ScheduleConfig {
}
