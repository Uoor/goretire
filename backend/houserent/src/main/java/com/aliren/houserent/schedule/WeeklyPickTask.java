package com.aliren.houserent.schedule;

import com.aliren.houserent.house.House;
import com.aliren.houserent.house.HouseMapper;
import com.aliren.houserent.robot.H5Links;
import com.aliren.houserent.robot.PushClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    /** H5 访问地址（配置 aliren.h5.base-url），用于卡片跳转；未配置时卡片不带跳转链接 */
    private final String h5BaseUrl;

    public WeeklyPickTask(HouseMapper houseMapper, PushClient pushClient,
            @Value("${aliren.h5.base-url:}") String h5BaseUrl) {
        this.houseMapper = houseMapper;
        this.pushClient = pushClient;
        this.h5BaseUrl = h5BaseUrl == null ? "" : h5BaseUrl.trim();
    }

    @Scheduled(cron = "${aliren.schedule.weekly-pick-cron:0 0 19 * * FRI}")
    public void weeklyPick() {
        LocalDateTime weekStart = LocalDate.now().minusDays(7).atStartOfDay();
        // 本周精选：本周新上架（7 天内创建）且仍在租的房源，最多 3 套
        List<House> picks = houseMapper.selectList(new QueryWrapper<House>()
                .eq("audit_status", House.AUDIT_ONLINE)
                .eq("rack_status", House.RACK_RENTING)
                .ge("created_at", weekStart)
                .orderByDesc("created_at")
                .last("LIMIT " + PICK_SIZE));
        // 本周新上架数概览：7 天内创建且审核通过的房源数
        long newThisWeek = houseMapper.selectCount(new QueryWrapper<House>()
                .eq("audit_status", House.AUDIT_ONLINE)
                .ge("created_at", weekStart));
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
                    .append("通勤：").append(h.getCommute() == null ? "" : h.getCommute());
            // 详情链接：落地页 + dingtalk page/link 协议（无 pc_slide，与群卡片一致）——
            // 移动端钉钉内置浏览器打开，PC 唤起钉钉，域名不暴露在浏览器地址栏
            String link = H5Links.landingUrl(h5BaseUrl, "/house/" + h.getId());
            if (!link.isBlank()) {
                md.append("<br/>👉 [查看房源详情](").append(H5Links.dingtalkLink(link)).append(")");
            }
            md.append("\n\n");
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
