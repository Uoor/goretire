package com.aliren.houserent.robot;

import com.aliren.houserent.demand.Demand;
import com.aliren.houserent.demand.DemandMapper;
import com.aliren.houserent.house.House;
import com.aliren.houserent.house.HouseMapper;
import com.aliren.houserent.match.MatchService;
import com.aliren.houserent.match.dto.DemandHit;
import com.aliren.houserent.match.dto.SubscriptionHit;
import com.aliren.houserent.pushlog.PushLogService;
import com.aliren.core.user.User;
import com.aliren.core.user.UserMapper;
import com.aliren.houserent.subscribe.Subscribe;
import com.aliren.houserent.subscribe.SubscribeMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 发布编排：房源审核通过后触发——
 * 1) 订阅批量匹配 + 求租墙匹配（LLM 或降级）
 * 2) 新上架卡片推送子群
 * 3) 命中者私聊提醒（工作通知）+ 推送历史落库（push_log）
 *
 * 注意：当前同步执行；接入真实 LLM 与推送后，建议将匹配+推送异步化（@Async / 消息队列），
 * 避免拖慢审核接口。
 */
@Slf4j
@Service
public class PublishOrchestrator {

    private final HouseMapper houseMapper;
    private final MatchService matchService;
    private final PushClient pushClient;
    private final PushLogService pushLogService;
    private final SubscribeMapper subscribeMapper;
    private final DemandMapper demandMapper;
    private final UserMapper userMapper;
    /** H5 访问地址（配置 aliren.h5.base-url），用于卡片跳转；未配置时卡片不带跳转按钮 */
    private final String h5BaseUrl;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    public PublishOrchestrator(HouseMapper houseMapper, MatchService matchService, PushClient pushClient,
                               PushLogService pushLogService, SubscribeMapper subscribeMapper,
                               DemandMapper demandMapper, UserMapper userMapper,
                               @Value("${aliren.h5.base-url:}") String h5BaseUrl) {
        this.houseMapper = houseMapper;
        this.matchService = matchService;
        this.pushClient = pushClient;
        this.pushLogService = pushLogService;
        this.subscribeMapper = subscribeMapper;
        this.demandMapper = demandMapper;
        this.userMapper = userMapper;
        this.h5BaseUrl = h5BaseUrl == null ? "" : h5BaseUrl.trim();
    }

    /** 房源审核通过后编排（幂等：仅对已上架房源生效） */
    public void onHouseAudited(Long houseId) {
        House h = houseMapper.selectById(houseId);
        if (h == null || h.getAuditStatus() != House.AUDIT_ONLINE) {
            return;
        }
        List<SubscriptionHit> subHits = matchService.matchSubscriptions(houseId);
        List<DemandHit> demandHits = matchService.matchDemands(houseId);
        // 新上架卡片：结构化 markdown + 「查看详情」按钮（配置 H5 地址后生效）
        String detailUrl = h5BaseUrl.isBlank() ? "" : h5BaseUrl + "/#/house/" + houseId;
        pushClient.sendGroupCardAction("🏠 新上架 · " + h.getCommunity(), buildHouseCard(h), detailUrl);
        for (SubscriptionHit hit : subHits) {
            Subscribe s = subscribeMapper.selectById(hit.getSubscribeId());
            if (s != null) {
                pushClient.sendWorkNotice(dingtalkUserId(s.getUserId()),
                        buildNoticeCard("🎯 订阅新匹配", h, detailUrl, hit.getReason()));
                String content = "你订阅的房源上新了：「" + h.getCommunity() + "」" + h.getHouseType()
                        + " " + h.getRent() + "元/月 —— " + hit.getReason();
                pushLogService.record(s.getUserId(), s.getId(), null, content);
                subscribeMapper.incrementPushCount(s.getId());
            }
        }
        for (DemandHit hit : demandHits) {
            Demand d = demandMapper.selectById(hit.getDemandId());
            if (d != null) {
                pushClient.sendWorkNotice(dingtalkUserId(d.getPublisherId()),
                        buildNoticeCard("🎯 求租新匹配", h, detailUrl, hit.getReason()));
                String content = "你挂在求租墙的需求有新房源：「" + h.getCommunity() + "」" + h.getHouseType()
                        + " " + h.getRent() + "元/月 —— " + hit.getReason();
                pushLogService.record(d.getPublisherId(), null, d.getId(), content);
            }
        }
        log.info("house {} audited: {} subscription hits, {} demand hits", houseId, subHits.size(), demandHits.size());
    }

    /** 业务用户 id → 钉钉 userid（工作通知目标；dev 桩用户返回其自身，真实钉钉用户返回钉钉身份） */
    private String dingtalkUserId(Long userId) {
        if (userId == null) {
            return "";
        }
        User u = userMapper.selectById(userId);
        return u == null ? "" : u.getDingtalkUserId();
    }

    /**
     * 结构化房源卡片（钉钉 markdown）。
     * 注意：钉钉 Webhook markdown 的单个 \n 不换行，必须用 <br/> 做行内换行，空行用 \n\n。
     * 封面图用 ![alt](url)，相对路径（/uploads/）按 h5BaseUrl 拼绝对地址。
     */
    private String buildHouseCard(House h) {
        return buildCardBody(h) + "<br/>✅ 已通过管理员审核，欢迎看房";
    }

    /**
     * 工作通知卡片（订阅/求租匹配）：标题行 + 房源结构化信息 + 跳转详情链接。
     * 未配置 H5 地址时省略链接，纯文本信息也能看。
     */
    private String buildNoticeCard(String title, House h, String detailUrl, String reason) {
        StringBuilder sb = new StringBuilder();
        sb.append("**").append(title).append("**<br/>");
        if (reason != null && !reason.isBlank()) {
            sb.append("📌 ").append(reason).append("<br/>");
        }
        sb.append(buildCardBody(h));
        if (detailUrl != null && !detailUrl.isBlank()) {
            sb.append("👉 [查看房源详情](").append(detailUrl).append(")");
        }
        return sb.toString();
    }

    /** 公共卡片体：封面 + 小区/户型/月租/区域/标签/通勤 */
    private String buildCardBody(House h) {
        StringBuilder sb = new StringBuilder();
        String cover = coverUrl(h);
        if (!cover.isBlank()) {
            sb.append("![🏠 房源封面](").append(cover).append(")\n\n");
        }
        sb.append("**小区**：").append(h.getCommunity()).append("<br/>");
        sb.append("**户型**：").append(h.getHouseType()).append(" · ").append(h.getArea()).append("㎡<br/>");
        sb.append("**月租**：**").append(rentText(h.getRent())).append(" 元/月**（").append(h.getDepositPay()).append("）<br/>");
        sb.append("**区域**：").append(h.getRegion()).append("<br/>");
        sb.append("**标签**：`").append(labelText(h.getLabel())).append("`");
        if (h.getPetOk() != null && h.getPetOk() == 1) {
            sb.append(" · `可养宠`");
        }
        sb.append("<br/>");
        if (h.getCommute() != null && !h.getCommute().isBlank()) {
            sb.append("**通勤**：🚲 ").append(h.getCommute()).append("<br/>");
        }
        sb.append("<br/>");
        return sb.toString();
    }

    /** 卡片封面图：images 首图；相对路径拼 H5 地址 */
    private String coverUrl(House h) {
        if (h.getImages() == null || h.getImages().isBlank()) {
            return "";
        }
        try {
            JsonNode arr = objectMapper.readTree(h.getImages());
            if (arr.isArray() && !arr.isEmpty()) {
                String url = arr.get(0).asText("").trim();
                if (url.isBlank()) {
                    return "";
                }
                if (url.startsWith("/")) {
                    return h5BaseUrl + url;
                }
                return url;
            }
        } catch (Exception ignored) {
            // 非法 JSON 忽略
        }
        return "";
    }

    /** 金额去尾零：6000.00 → 6000 */
    private String rentText(java.math.BigDecimal rent) {
        if (rent == null) {
            return "";
        }
        return rent.stripTrailingZeros().toPlainString();
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
