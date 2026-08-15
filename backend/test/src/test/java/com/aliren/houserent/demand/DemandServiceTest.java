package com.aliren.houserent.demand;

import com.aliren.core.common.BusinessException;
import com.aliren.houserent.demand.dto.DemandCreateRequest;
import com.aliren.houserent.demand.dto.DemandListQuery;
import com.aliren.houserent.demand.dto.DemandResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemandServiceTest {

    @Mock
    private DemandMapper demandMapper;

    private DemandService demandService;

    @BeforeEach
    void setUp() {
        demandService = new DemandService(demandMapper);
    }

    @Test
    void create_initializesPendingStatus() {
        DemandCreateRequest req = new DemandCreateRequest();
        req.setRegion("杭州西溪");
        req.setHouseType("2室1厅");
        req.setBudget("{\"min\":4000,\"max\":6000}");
        when(demandMapper.insert(any(Demand.class))).thenAnswer(inv -> {
            inv.getArgument(0, Demand.class).setId(5L);
            return 1;
        });

        Long id = demandService.create(7L, req);
        assertThat(id).isEqualTo(5L);

        ArgumentCaptor<Demand> captor = ArgumentCaptor.forClass(Demand.class);
        verify(demandMapper).insert(captor.capture());
        Demand saved = captor.getValue();
        assertThat(saved.getPublisherId()).isEqualTo(7L);
        assertThat(saved.getMatchStatus()).isZero(); // 待匹配
    }

    @Test
    void list_wallExcludesDoneDemands() {
        Demand pending = new Demand();
        pending.setId(1L);
        pending.setMatchStatus(0);
        when(demandMapper.selectList(any())).thenReturn(List.of(pending));

        List<DemandResponse> list = demandService.list(new DemandListQuery());
        assertThat(list).hasSize(1);
    }

    @Test
    void withdraw_nonOwner_rejected() {
        Demand d = new Demand();
        d.setId(1L);
        d.setPublisherId(7L);
        when(demandMapper.selectById(1L)).thenReturn(d);

        assertThatThrownBy(() -> demandService.withdraw(8L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权限");
    }

    @Test
    void withdraw_owner_succeeds() {
        Demand d = new Demand();
        d.setId(1L);
        d.setPublisherId(7L);
        when(demandMapper.selectById(1L)).thenReturn(d);

        demandService.withdraw(7L, 1L);
        verify(demandMapper).deleteById(1L);
    }

    @Test
    void complete_setsDoneStatus() {
        Demand d = new Demand();
        d.setId(1L);
        d.setPublisherId(7L);
        d.setMatchStatus(0);
        when(demandMapper.selectById(1L)).thenReturn(d);

        demandService.complete(7L, 1L);
        ArgumentCaptor<Demand> captor = ArgumentCaptor.forClass(Demand.class);
        verify(demandMapper).updateById(captor.capture());
        assertThat(captor.getValue().getMatchStatus()).isEqualTo(Demand.STATUS_DONE);
    }

    @Test
    void detail_notFound_rejected() {
        when(demandMapper.selectById(99L)).thenReturn(null);
        assertThatThrownBy(() -> demandService.detail(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在");
    }
}
