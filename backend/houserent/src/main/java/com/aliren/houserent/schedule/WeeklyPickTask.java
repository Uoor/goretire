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
 * 周五精选周推：
 * - 本周（7 天内）新上架且在线在租房源 ≥7 套 → 精选前 3 套；
 * - 本周不足 7 套 → 回退展示全站最新 5 套在线在租房源，避免周推冷场。
 * 另附本周新上架数概览 → 推送子群。
 * cron：每周五 19:00。
 */
@Slf4j
@Component
public class WeeklyPickTask {

    private static final int PICK_SIZE = 3;
    /** 本周房源达到该数量时按本周精选；否则回退全站最新 */
    private static final int WEEK_THRESHOLD = 7;
    /** 回退时展示的房源条数 */
    private static final int FALLBACK_SIZE = 5;

    private final HouseMapper houseMapper;
    private final PushClient pushClient;
    /** H5 访问地址（配置 aliren.h5.base-url），用于卡片跳转；未配置时卡片不带跳转链接 */
    private final String h5BaseUrl;
    /** 租房群入群链接（钉钉群二维码链接，如 https://qr.dingtalk.com/action/joingroup?code=...） */
    private final String groupInviteUrl;

    public WeeklyPickTask(HouseMapper houseMapper, PushClient pushClient,
            @Value("${aliren.h5.base-url:}") String h5BaseUrl,
            @Value("${aliren.robot.group-invite-url:}") String groupInviteUrl) {
        this.houseMapper = houseMapper;
        this.pushClient = pushClient;
        this.h5BaseUrl = h5BaseUrl == null ? "" : h5BaseUrl.trim();
        this.groupInviteUrl = groupInviteUrl == null ? "" : groupInviteUrl.trim();
    }

    @Scheduled(cron = "${aliren.schedule.weekly-pick-cron:0 0 19 * * FRI}")
    public void weeklyPick() {
        LocalDateTime weekStart = LocalDate.now().minusDays(7).atStartOfDay();
        // 本周精选候选：本周（7 天内）新上架且在线在租
        List<House> weekPicks = houseMapper.selectList(new QueryWrapper<House>()
                .eq("audit_status", House.AUDIT_ONLINE)
                .eq("rack_status", House.RACK_RENTING)
                .ge("created_at", weekStart)
                .orderByDesc("created_at"));
        List<House> picks;
        if (weekPicks.size() >= WEEK_THRESHOLD) {
            // 本周房源充足：精选最新 3 套
            picks = weekPicks.stream().limit(PICK_SIZE).toList();
        } else {
            // 本周房源少于 7：回退展示全站最新 5 套在线在租，保证周推不冷场
            log.info("weekly pick fallback: only {} houses this week, use latest {}", weekPicks.size(), FALLBACK_SIZE);
            picks = houseMapper.selectList(new QueryWrapper<House>()
                    .eq("audit_status", House.AUDIT_ONLINE)
                    .eq("rack_status", House.RACK_RENTING)
                    .orderByDesc("created_at")
                    .last("LIMIT " + FALLBACK_SIZE));
        }
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
        // 租房群入群入口：钉钉群二维码链接（钉钉容器内点击直接唤起加群页）
        if (!groupInviteUrl.isBlank()) {
            md.append("🏠 [加入租房群](").append(H5Links.dingtalkLink(groupInviteUrl)).append(")");
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
