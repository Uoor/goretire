package com.aliren.houserent.subscribe;

import com.aliren.core.common.BusinessException;
import com.aliren.houserent.subscribe.dto.SubscribeCreateRequest;
import com.aliren.houserent.subscribe.dto.SubscribeResponse;
import com.aliren.houserent.subscribe.dto.SubscribeUpdateRequest;
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
class SubscribeServiceTest {

    @Mock
    private SubscribeMapper subscribeMapper;
    @Mock
    private com.aliren.houserent.pushlog.PushLogService pushLogService;
    @Mock
    private SubscriptionParser subscriptionParser;

    private SubscribeService subscribeService;

    @BeforeEach
    void setUp() {
        subscribeService = new SubscribeService(subscribeMapper, pushLogService, subscriptionParser);
    }

    @Test
    void create_initializesActiveStatus() {
        SubscribeCreateRequest req = new SubscribeCreateRequest();
        req.setType(Subscribe.TYPE_FIND_HOUSE);
        req.setRawText("西溪附近 6000 以内两居，能养猫");
        when(subscribeMapper.insert(any(Subscribe.class))).thenAnswer(inv -> {
            inv.getArgument(0, Subscribe.class).setId(3L);
            return 1;
        });

        Long id = subscribeService.create(7L, req);
        assertThat(id).isEqualTo(3L);

        ArgumentCaptor<Subscribe> captor = ArgumentCaptor.forClass(Subscribe.class);
        verify(subscribeMapper).insert(captor.capture());
        Subscribe saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(7L);
        assertThat(saved.getStatus()).isZero(); // 活跃
        assertThat(saved.getPushCount()).isZero();
    }

    @Test
    void list_excludesDeleted() {
        Subscribe active = new Subscribe();
        active.setId(1L);
        active.setStatus(0);
        when(subscribeMapper.selectList(any())).thenReturn(List.of(active));

        List<SubscribeResponse> list = subscribeService.list(7L);
        assertThat(list).hasSize(1);
    }

    @Test
    void update_pause_nonOwner_rejected() {
        Subscribe s = new Subscribe();
        s.setId(1L);
        s.setUserId(7L);
        s.setStatus(Subscribe.STATUS_ACTIVE);
        when(subscribeMapper.selectById(1L)).thenReturn(s);

        SubscribeUpdateRequest req = new SubscribeUpdateRequest();
        req.setStatus(Subscribe.STATUS_PAUSED);
        assertThatThrownBy(() -> subscribeService.update(8L, 1L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权限");
    }

    @Test
    void update_pause_owner_succeeds() {
        Subscribe s = new Subscribe();
        s.setId(1L);
        s.setUserId(7L);
        s.setStatus(Subscribe.STATUS_ACTIVE);
        when(subscribeMapper.selectById(1L)).thenReturn(s);

        SubscribeUpdateRequest req = new SubscribeUpdateRequest();
        req.setStatus(Subscribe.STATUS_PAUSED);
        SubscribeResponse resp = subscribeService.update(7L, 1L, req);
        assertThat(resp.getStatus()).isEqualTo(Subscribe.STATUS_PAUSED);
        verify(subscribeMapper).updateById(any(Subscribe.class));
    }

    @Test
    void update_invalidStatus_rejected() {
        Subscribe s = new Subscribe();
        s.setId(1L);
        s.setUserId(7L);
        s.setStatus(Subscribe.STATUS_ACTIVE);
        when(subscribeMapper.selectById(1L)).thenReturn(s);

        SubscribeUpdateRequest req = new SubscribeUpdateRequest();
        req.setStatus(99);
        assertThatThrownBy(() -> subscribeService.update(7L, 1L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("状态不合法");
    }

    @Test
    void delete_softDeletes() {
        Subscribe s = new Subscribe();
        s.setId(1L);
        s.setUserId(7L);
        s.setStatus(Subscribe.STATUS_ACTIVE);
        when(subscribeMapper.selectById(1L)).thenReturn(s);

        subscribeService.delete(7L, 1L);
        ArgumentCaptor<Subscribe> captor = ArgumentCaptor.forClass(Subscribe.class);
        verify(subscribeMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(Subscribe.STATUS_DELETED);
    }

    @Test
    void incrementPushCount_increments() {
        Subscribe s = new Subscribe();
        s.setId(1L);
        s.setPushCount(3);
        when(subscribeMapper.selectById(1L)).thenReturn(s);

        subscribeService.incrementPushCount(1L);
        ArgumentCaptor<Subscribe> captor = ArgumentCaptor.forClass(Subscribe.class);
        verify(subscribeMapper).updateById(captor.capture());
        assertThat(captor.getValue().getPushCount()).isEqualTo(4);
    }
}
