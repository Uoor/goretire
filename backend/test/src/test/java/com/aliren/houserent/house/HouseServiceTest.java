package com.aliren.houserent.house;

import com.aliren.core.common.BusinessException;
import com.aliren.houserent.house.dto.HouseCreateRequest;
import com.aliren.houserent.house.dto.HouseResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HouseServiceTest {

    @Mock
    private HouseMapper houseMapper;
    @Mock
    private com.aliren.core.user.UserMapper userMapper;

    private HouseService houseService;

    @BeforeEach
    void setUp() {
        houseService = new HouseService(houseMapper, userMapper);
    }

    @Test
    void publish_createsHouseWithPendingAudit() {
        HouseCreateRequest req = new HouseCreateRequest();
        req.setCommunity("西溪八方城");
        req.setRegion("杭州西溪");
        req.setHouseType("2室1厅");
        req.setArea(89);
        req.setRent(BigDecimal.valueOf(5800));
        req.setDepositPay("押一付三");
        req.setLabel(1);
        when(houseMapper.insert(any(House.class))).thenAnswer(inv -> {
            inv.getArgument(0, House.class).setId(10L);
            return 1;
        });

        Long id = houseService.publish(7L, req);
        assertThat(id).isEqualTo(10L);

        ArgumentCaptor<House> captor = ArgumentCaptor.forClass(House.class);
        verify(houseMapper).insert(captor.capture());
        House saved = captor.getValue();
        assertThat(saved.getPublisherId()).isEqualTo(7L);
        assertThat(saved.getAuditStatus()).isZero(); // 待审核
        assertThat(saved.getRackStatus()).isZero();  // 在租中
    }

    @Test
    void publish_negativeRent_rejected() {
        HouseCreateRequest req = new HouseCreateRequest();
        req.setRent(BigDecimal.valueOf(-1));
        assertThatThrownBy(() -> houseService.publish(7L, req))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void detail_notAudited_forbidden() {
        House h = new House();
        h.setId(1L);
        h.setAuditStatus(0);
        when(houseMapper.selectById(1L)).thenReturn(h);
        assertThatThrownBy(() -> houseService.detail(1L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void detail_missing_404() {
        when(houseMapper.selectById(99L)).thenReturn(null);
        assertThatThrownBy(() -> houseService.detail(99L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void list_auditedOnly() {
        when(houseMapper.selectList(any())).thenReturn(List.of());
        List<HouseResponse> list = houseService.list(new com.aliren.houserent.house.dto.HouseListQuery());
        assertThat(list).isEmpty();
    }
}
