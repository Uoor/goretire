package com.aliren.houserent.robot;

import com.aliren.houserent.house.House;
import com.aliren.houserent.house.HouseMapper;
import com.aliren.houserent.match.MatchService;
import com.aliren.houserent.match.dto.DemandHit;
import com.aliren.houserent.match.dto.SubscriptionHit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
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

    private PublishOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new PublishOrchestrator(houseMapper, matchService, pushClient);
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

        orchestrator.onHouseAudited(1L);

        verify(pushClient).sendGroupCard(anyString(), anyString());
        verify(pushClient).sendWorkNotice("subscribe#3", "你订阅的房源上新了：「西溪八方城」2室1厅 5800元/月 —— 区域预算都符合");
        verify(pushClient).sendWorkNotice("demand#5", "你挂在求租墙的需求有新房源：「西溪八方城」2室1厅 5800元/月 —— 目标区域一致");
    }

    @Test
    void onHouseAudited_notOnline_skips() {
        House h = onlineHouse(1L);
        h.setAuditStatus(House.AUDIT_PENDING);
        when(houseMapper.selectById(1L)).thenReturn(h);

        orchestrator.onHouseAudited(1L);
        verify(pushClient, never()).sendGroupCard(anyString(), anyString());
    }
}
