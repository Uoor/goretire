package com.aliren.houserent.house;

import com.aliren.core.common.BusinessException;
import com.aliren.core.user.User;
import com.aliren.core.user.UserMapper;
import com.aliren.houserent.pushlog.PushLogService;
import com.aliren.houserent.robot.PushClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContactServiceTest {

    @Mock
    private HouseMapper houseMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PushClient pushClient;
    @Mock
    private PushLogService pushLogService;

    private ContactService service;

    @BeforeEach
    void setUp() {
        service = new ContactService(houseMapper, userMapper, pushClient, pushLogService);
    }

    private House onlineHouse() {
        House h = new House();
        h.setId(1L);
        h.setPublisherId(3L);
        h.setCommunity("西溪八方城");
        h.setHouseType("2室1厅");
        h.setArea(89);
        h.setRent(java.math.BigDecimal.valueOf(5800));
        h.setRegion("杭州西溪");
        h.setAuditStatus(House.AUDIT_ONLINE);
        return h;
    }

    @Test
    void contact_sendsWorkNoticeToOwner() {
        when(houseMapper.selectById(1L)).thenReturn(onlineHouse());
        User owner = new User();
        owner.setId(3L);
        owner.setNickname("房东老王");
        owner.setDingtalkUserId("ding-owner-1");
        when(userMapper.selectById(3L)).thenReturn(owner);

        String name = service.contact(9L, 1L);
        assertThat(name).isEqualTo("房东老王");
        verify(pushClient).sendWorkNotice("ding-owner-1", "有校友对你的房源感兴趣：「西溪八方城」2室1厅 89㎡ 5800元/月（杭州西溪），可在钉钉内与对方沟通。");
        verify(pushLogService).record(3L, null, null, "有校友对你的房源感兴趣：「西溪八方城」2室1厅 89㎡ 5800元/月（杭州西溪），可在钉钉内与对方沟通。");
    }

    @Test
    void contact_notOnline_rejected() {
        House h = onlineHouse();
        h.setAuditStatus(House.AUDIT_PENDING);
        when(houseMapper.selectById(1L)).thenReturn(h);
        assertThatThrownBy(() -> service.contact(9L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("不存在或未上架");
    }

    @Test
    void contact_ownerNoDingtalk_rejected() {
        when(houseMapper.selectById(1L)).thenReturn(onlineHouse());
        User owner = new User();
        owner.setId(3L);
        when(userMapper.selectById(3L)).thenReturn(owner);
        assertThatThrownBy(() -> service.contact(9L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("钉钉通知");
    }
}
