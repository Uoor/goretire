package com.aliren.houserent.match;

import com.aliren.houserent.demand.Demand;
import com.aliren.houserent.demand.DemandMapper;
import com.aliren.houserent.house.House;
import com.aliren.houserent.house.HouseMapper;
import com.aliren.houserent.match.client.MatchClient;
import com.aliren.houserent.match.dto.DemandHit;
import com.aliren.houserent.match.dto.HouseMatch;
import com.aliren.houserent.match.dto.MatchSearchResponse;
import com.aliren.houserent.match.dto.SubscriptionHit;
import com.aliren.houserent.subscribe.Subscribe;
import com.aliren.houserent.subscribe.SubscribeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private HouseMapper houseMapper;
    @Mock
    private DemandMapper demandMapper;
    @Mock
    private SubscribeMapper subscribeMapper;
    @Mock
    private MatchClient matchClient;

    private MatchService matchService;

    @BeforeEach
    void setUp() {
        matchService = new MatchService(houseMapper, demandMapper, subscribeMapper, matchClient);
    }

    private House house(Long id, String region, String rent) {
        House h = new House();
        h.setId(id);
        h.setCommunity("西溪八方城");
        h.setRegion(region);
        h.setHouseType("2室1厅");
        h.setArea(89);
        h.setRent(new BigDecimal(rent));
        h.setLabel(1);
        h.setAuditStatus(House.AUDIT_ONLINE);
        h.setRackStatus(House.RACK_RENTING);
        return h;
    }

    @Test
    void search_llmPath_parsesJsonOutput() {
        when(houseMapper.selectList(any())).thenReturn(List.of(house(1L, "杭州西溪", "5800")));
        when(matchClient.complete(anyString())).thenReturn("[{\"houseId\":1,\"reason\":\"近西溪园区，可养猫\"}]");

        MatchSearchResponse resp = matchService.searchHouses("西溪附近 6000 以内两居，能养猫");
        assertThat(resp.isDegraded()).isFalse();
        assertThat(resp.getMatches()).hasSize(1);
        assertThat(resp.getMatches().get(0).getHouseId()).isEqualTo(1L);
    }

    @Test
    void search_llmUnavailable_fallsBackToLocalFilter() {
        when(houseMapper.selectList(any())).thenReturn(List.of(
                house(1L, "杭州西溪", "5800"),
                house(2L, "北京望京", "7500")));
        when(matchClient.complete(anyString())).thenReturn(null);

        MatchSearchResponse resp = matchService.searchHouses("西溪 6000 以内");
        assertThat(resp.isDegraded()).isTrue();
        // 降级：只保留区域命中且预算内的房源
        assertThat(resp.getMatches()).extracting(HouseMatch::getHouseId).containsExactly(1L);
    }

    @Test
    void search_noCandidates_emptyResult() {
        when(houseMapper.selectList(any())).thenReturn(List.of());
        MatchSearchResponse resp = matchService.searchHouses("随便看看");
        assertThat(resp.getMatches()).isEmpty();
        assertThat(resp.isDegraded()).isFalse();
    }

    @Test
    void matchSubscriptions_llmPath_parsesHits() {
        House h = house(1L, "杭州西溪", "5800");
        when(houseMapper.selectById(1L)).thenReturn(h);
        Subscribe s = new Subscribe();
        s.setId(3L);
        s.setType(Subscribe.TYPE_FIND_HOUSE);
        s.setRawText("西溪 6000 以内");
        when(subscribeMapper.selectList(any())).thenReturn(List.of(s));
        when(matchClient.complete(anyString())).thenReturn("[{\"subscribeId\":3,\"reason\":\"区域预算都符合\"}]");

        List<SubscriptionHit> hits = matchService.matchSubscriptions(1L);
        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).getSubscribeId()).isEqualTo(3L);
    }

    @Test
    void matchSubscriptions_fallback_byRegion() {
        House h = house(1L, "杭州西溪", "5800");
        when(houseMapper.selectById(1L)).thenReturn(h);
        Subscribe match = new Subscribe();
        match.setId(1L);
        match.setType(Subscribe.TYPE_FIND_HOUSE);
        match.setRawText("想找西溪的房子");
        Subscribe miss = new Subscribe();
        miss.setId(2L);
        miss.setType(Subscribe.TYPE_FIND_HOUSE);
        miss.setRawText("北京望京两居");
        when(subscribeMapper.selectList(any())).thenReturn(List.of(match, miss));
        when(matchClient.complete(anyString())).thenReturn(null);

        List<SubscriptionHit> hits = matchService.matchSubscriptions(1L);
        assertThat(hits).extracting(SubscriptionHit::getSubscribeId).containsExactly(1L);
    }

    @Test
    void matchDemands_fallback_byRegion() {
        House h = house(1L, "杭州西溪", "5800");
        when(houseMapper.selectById(1L)).thenReturn(h);
        Demand d = new Demand();
        d.setId(5L);
        d.setRegion("杭州西溪");
        d.setMatchStatus(Demand.STATUS_PENDING);
        when(demandMapper.selectList(any())).thenReturn(List.of(d));
        when(matchClient.complete(anyString())).thenReturn(null);

        List<DemandHit> hits = matchService.matchDemands(1L);
        assertThat(hits).extracting(DemandHit::getDemandId).containsExactly(5L);
    }
}
