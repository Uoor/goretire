package com.aliren.houserent.robot;

import com.aliren.houserent.house.House;
import com.aliren.houserent.house.HouseMapper;
import com.aliren.houserent.match.MatchService;
import com.aliren.houserent.match.dto.DemandHit;
import com.aliren.houserent.demand.Demand;
import com.aliren.houserent.match.dto.SubscriptionHit;
import com.aliren.houserent.subscribe.Subscribe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublishOrchestratorTest {

    @Mock
    private HouseMapper houseMapper;
    @Mock
    private MatchService matchService;
    @Mock
    private PushClient pushClient;
    @Mock
    private com.aliren.houserent.pushlog.PushLogService pushLogService;
    @Mock
    private com.aliren.houserent.subscribe.SubscribeMapper subscribeMapper;
    @Mock
    private com.aliren.houserent.demand.DemandMapper demandMapper;
    @Mock
    private com.aliren.core.user.UserMapper userMapper;

    private PublishOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new PublishOrchestrator(houseMapper, matchService, pushClient,
                pushLogService, subscribeMapper, demandMapper, userMapper,
                "https://h5.example.com", 3);
    }

    private House onlineHouse(Long id) {
        House h = new House();
        h.setId(id);
        h.setCommunity("西溪八方城");
        h.setHouseType("2室1厅");
        h.setArea(89);
        h.setRent(BigDecimal.valueOf(5800));
        h.setDepositPay("押一付三");
        h.setRegion("杭州西溪");
        h.setLabel(House.LABEL_DIRECT);
        h.setPetOk(1);
        h.setAuditStatus(House.AUDIT_ONLINE);
        return h;
    }

    @Test
    void onHouseAudited_matchesAndPushes() {
        when(houseMapper.selectById(1L)).thenReturn(onlineHouse(1L));
        when(matchService.matchSubscriptions(1L))
                .thenReturn(List.of(new SubscriptionHit(3L, "区域预算都符合")));
        when(matchService.matchDemands(1L))
                .thenReturn(List.of(new DemandHit(5L, "目标区域一致")));
        Subscribe sub = new Subscribe();
        sub.setId(3L);
        sub.setUserId(7L);
        when(subscribeMapper.selectById(3L)).thenReturn(sub);
        Demand demand = new Demand();
        demand.setId(5L);
        demand.setPublisherId(8L);
        when(demandMapper.selectById(5L)).thenReturn(demand);
        com.aliren.core.user.User u1 = new com.aliren.core.user.User();
        u1.setId(7L);
        u1.setDingtalkUserId("ding-user-7");
        com.aliren.core.user.User u2 = new com.aliren.core.user.User();
        u2.setId(8L);
        u2.setDingtalkUserId("ding-user-8");
        when(userMapper.selectById(7L)).thenReturn(u1);
        when(userMapper.selectById(8L)).thenReturn(u2);

        orchestrator.onHouseAudited(1L);

        verify(pushClient).sendGroupCardAction(anyString(), anyString(), eq("https://h5.example.com/#/house/1"));
        // 工作通知升级为结构化卡片：标题 + 命中理由 + 房源信息 + 跳转链接
        verify(pushClient).sendWorkNotice(eq("ding-user-7"), org.mockito.ArgumentMatchers.contains("🎯 订阅新匹配"));
        verify(pushClient).sendWorkNotice(eq("ding-user-8"), org.mockito.ArgumentMatchers.contains("🎯 求租新匹配"));
        verify(pushClient).sendWorkNotice(eq("ding-user-7"), org.mockito.ArgumentMatchers.contains("区域预算都符合"));
        // 工作通知详情链接 → 钉钉 page/link 协议，pc_slide=true（PC 侧边栏 / 移动端内置浏览器）
        verify(pushClient).sendWorkNotice(eq("ding-user-7"), org.mockito.ArgumentMatchers.contains("dingtalk://dingtalkclient/page/link"));
        verify(pushClient).sendWorkNotice(eq("ding-user-7"), org.mockito.ArgumentMatchers.contains("pc_slide=true"));
        verify(pushClient).sendWorkNotice(eq("ding-user-7"), org.mockito.ArgumentMatchers.contains("https%3A%2F%2Fh5.example.com"));
    }

    @Test
    void onHouseAudited_notOnline_skips() {
        House h = onlineHouse(1L);
        h.setAuditStatus(House.AUDIT_PENDING);
        when(houseMapper.selectById(1L)).thenReturn(h);

        orchestrator.onHouseAudited(1L);
        verify(pushClient, never()).sendGroupCardAction(anyString(), anyString(), anyString());
    }
}
