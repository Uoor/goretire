package com.aliren.houserent.schedule;

import com.aliren.houserent.house.House;
import com.aliren.houserent.house.HouseMapper;
import com.aliren.houserent.robot.PushClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 周五精选周推：本周精选 3 套（MVP：最近上架且仍在租）+ 本周新上架数概览 → 推送子群。
 * cron：每周五 19:00。
 */
@Slf4j
@Component
public class WeeklyPickTask {

    private static final int PICK_SIZE = 3;

    private final HouseMapper houseMapper;
    private final PushClient pushClient;

    public WeeklyPickTask(HouseMapper houseMapper, PushClient pushClient) {
        this.houseMapper = houseMapper;
        this.pushClient = pushClient;
    }

    @Scheduled(cron = "${aliren.schedule.weekly-pick-cron:0 0 19 * * FRI}")
    public void weeklyPick() {
        List<House> picks = houseMapper.selectList(new QueryWrapper<House>()
                .eq("audit_status", House.AUDIT_ONLINE)
                .eq("rack_status", House.RACK_RENTING)
                .orderByDesc("created_at")
                .last("LIMIT " + PICK_SIZE));
        long newThisWeek = houseMapper.selectCount(new QueryWrapper<House>()
                .ge("created_at", LocalDate.now().minusDays(7).atStartOfDay()));
        if (picks.isEmpty()) {
            log.info("weekly pick skipped: no online houses");
            return;
        }
        StringBuilder md = new StringBuilder("📅 **本周精选房源**（本周新上架 " + newThisWeek + " 套）\n\n");
        int i = 1;
        for (House h : picks) {
            md.append(i++).append(". **").append(h.getCommunity()).append("** ")
                    .append(h.getHouseType()).append(" ").append(h.getArea()).append("㎡<br/>")
                    .append("月租 **").append(h.getRentText()).append(" 元** · ").append(h.getRegion())
                    .append(" · `").append(labelText(h.getLabel())).append("`<br/>")
                    .append("通勤：").append(h.getCommute() == null ? "" : h.getCommute()).append("\n\n");
        }
        pushClient.sendGroupCard("本周精选", md.toString());
    }

    private String labelText(Integer label) {
        return switch (label == null ? 0 : label) {
            case House.LABEL_DIRECT -> "房东直租";
            case House.LABEL_TRANSFER -> "校友转租";
            case House.LABEL_SHARE -> "合租拼室友";
            default -> "";
        };
    }
}
