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
import lombok.extern.slf4j.Slf4j;
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

    public PublishOrchestrator(HouseMapper houseMapper, MatchService matchService, PushClient pushClient,
                               PushLogService pushLogService, SubscribeMapper subscribeMapper,
                               DemandMapper demandMapper, UserMapper userMapper) {
        this.houseMapper = houseMapper;
        this.matchService = matchService;
        this.pushClient = pushClient;
        this.pushLogService = pushLogService;
        this.subscribeMapper = subscribeMapper;
        this.demandMapper = demandMapper;
        this.userMapper = userMapper;
    }

    /** 房源审核通过后编排（幂等：仅对已上架房源生效） */
    public void onHouseAudited(Long houseId) {
        House h = houseMapper.selectById(houseId);
        if (h == null || h.getAuditStatus() != House.AUDIT_ONLINE) {
            return;
        }
        List<SubscriptionHit> subHits = matchService.matchSubscriptions(houseId);
        List<DemandHit> demandHits = matchService.matchDemands(houseId);
        pushClient.sendGroupCard("新上架", buildHouseCard(h));
        for (SubscriptionHit hit : subHits) {
            String content = "你订阅的房源上新了：「" + h.getCommunity() + "」" + h.getHouseType()
                    + " " + h.getRent() + "元/月 —— " + hit.getReason();
            Subscribe s = subscribeMapper.selectById(hit.getSubscribeId());
            if (s != null) {
                pushClient.sendWorkNotice(dingtalkUserId(s.getUserId()), content);
                pushLogService.record(s.getUserId(), s.getId(), null, content);
            }
        }
        for (DemandHit hit : demandHits) {
            String content = "你挂在求租墙的需求有新房源：「" + h.getCommunity() + "」" + h.getHouseType()
                    + " " + h.getRent() + "元/月 —— " + hit.getReason();
            Demand d = demandMapper.selectById(hit.getDemandId());
            if (d != null) {
                pushClient.sendWorkNotice(dingtalkUserId(d.getPublisherId()), content);
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

    private String buildHouseCard(House h) {
        return "🏠 **" + h.getCommunity() + "** " + h.getHouseType() + " " + h.getArea() + "㎡\n"
                + "月租 " + h.getRent() + " 元 · " + h.getDepositPay() + " · " + h.getRegion() + "\n"
                + "标签：" + labelText(h.getLabel())
                + (h.getPetOk() != null && h.getPetOk() == 1 ? " · 可养宠" : "")
                + "\n通勤：" + (h.getCommute() == null ? "" : h.getCommute())
                + "\n[查看详情 →]";
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
