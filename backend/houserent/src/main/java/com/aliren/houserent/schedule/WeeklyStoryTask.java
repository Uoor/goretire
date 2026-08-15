package com.aliren.houserent.schedule;

import com.aliren.houserent.house.House;
import com.aliren.houserent.house.HouseMapper;
import com.aliren.houserent.robot.PushClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 周一安居故事：统计上周「在安居找到新家」（feedback_answer=1）的校友数 → 推送子群。
 * 数据来自发布人下架时的轻问句回答。
 * cron：每周一 10:00。
 */
@Slf4j
@Component
public class WeeklyStoryTask {

    private final HouseMapper houseMapper;
    private final PushClient pushClient;

    public WeeklyStoryTask(HouseMapper houseMapper, PushClient pushClient) {
        this.houseMapper = houseMapper;
        this.pushClient = pushClient;
    }

    @Scheduled(cron = "${aliren.schedule.weekly-story-cron:0 0 10 * * MON}")
    public void weeklyStory() {
        long found = houseMapper.selectCount(new QueryWrapper<House>()
                .eq("feedback_answer", 1)
                .ge("updated_at", LocalDate.now().minusDays(7).atStartOfDay()));
        String md = "🎉 **本周安居故事**\n\n上周有 **" + found + "** 位校友通过「校友安居」找到了新家。\n"
                + "真实房源 · 校友互信 · 免费直租，安居让每一次换城都有托底。";
        pushClient.sendGroupCard("安居故事", md);
        log.info("weekly story pushed: {} alumni found home", found);
    }
}
