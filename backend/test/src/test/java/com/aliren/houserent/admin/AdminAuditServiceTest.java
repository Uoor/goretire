package com.aliren.houserent.admin;

import com.aliren.core.common.BusinessException;
import com.aliren.houserent.house.House;
import com.aliren.houserent.house.HouseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuditServiceTest {

    @Mock
    private HouseMapper houseMapper;
    @Mock
    private com.aliren.houserent.auditlog.AuditLogService auditLogService;
    @Mock
    private com.aliren.houserent.report.ReportService reportService;
    @Mock
    private com.aliren.houserent.robot.PublishOrchestrator publishOrchestrator;

    private AdminAuditService service;

    @BeforeEach
    void setUp() {
        service = new AdminAuditService(houseMapper, auditLogService, reportService, publishOrchestrator);
    }

    @Test
    void audit_nonAdmin_rejected() {
        assertThatThrownBy(() -> service.audit(8L, 0, 1L, true, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权限");
    }

    @Test
    void audit_pass_setsAudited() {
        House h = new House();
        h.setId(1L);
        h.setAuditStatus(0);
        when(houseMapper.selectById(1L)).thenReturn(h);

        service.audit(2L, 1, 1L, true, null);
        verify(houseMapper).updateById(any(House.class));
    }

    @Test
    void audit_reject_requiresReason() {
        House h = new House();
        h.setId(1L);
        h.setAuditStatus(0);
        when(houseMapper.selectById(1L)).thenReturn(h);

        assertThatThrownBy(() -> service.audit(2L, 1, 1L, false, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("驳回原因");
    }

    @Test
    void audit_notPending_404() {
        House h = new House();
        h.setId(1L);
        h.setAuditStatus(1); // 已上架，非待审核
        when(houseMapper.selectById(1L)).thenReturn(h);

        assertThatThrownBy(() -> service.audit(2L, 1, 1L, true, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void offRack_nonOwnerNonAdmin_rejected() {
        House h = new House();
        h.setId(1L);
        h.setPublisherId(7L);
        h.setAuditStatus(1);
        when(houseMapper.selectById(1L)).thenReturn(h);

        assertThatThrownBy(() -> service.offRack(8L, 0, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权限");
    }

    @Test
    void offRack_owner_succeeds() {
        House h = new House();
        h.setId(1L);
        h.setPublisherId(7L);
        h.setAuditStatus(1);
        when(houseMapper.selectById(1L)).thenReturn(h);

        service.offRack(7L, 0, 1L);
        verify(houseMapper).updateById(any(House.class));
    }

    @Test
    void offRack_admin_succeeds() {
        House h = new House();
        h.setId(1L);
        h.setPublisherId(7L);
        h.setAuditStatus(1);
        when(houseMapper.selectById(1L)).thenReturn(h);

        service.offRack(9L, 1, 1L);
        verify(houseMapper).updateById(any(House.class));
    }

    @Test
    void reList_rentedHouse_backToRenting() {
        House h = new House();
        h.setId(1L);
        h.setPublisherId(7L);
        h.setAuditStatus(1);
        h.setRackStatus(1); // 已租出
        when(houseMapper.selectById(1L)).thenReturn(h);

        service.reList(7L, 0, 1L);
        ArgumentCaptor<House> captor = ArgumentCaptor.forClass(House.class);
        verify(houseMapper).updateById(captor.capture());
        assertThat(captor.getValue().getRackStatus()).isZero(); // 回到在租中
    }

    @Test
    void reList_notOnline_rejected() {
        House h = new House();
        h.setId(1L);
        h.setPublisherId(7L);
        h.setAuditStatus(2); // 已驳回
        when(houseMapper.selectById(1L)).thenReturn(h);

        assertThatThrownBy(() -> service.reList(7L, 0, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已上架");
    }

    @Test
    void deleteHouse_rentedHouse_deletes() {
        House h = new House();
        h.setId(1L);
        h.setPublisherId(7L);
        h.setAuditStatus(1);
        h.setRackStatus(1); // 已租出，可删
        when(houseMapper.selectById(1L)).thenReturn(h);

        service.deleteHouse(7L, 0, 1L);
        verify(houseMapper).deleteById(1L);
    }

    @Test
    void deleteHouse_activeRenting_deletes() {
        House h = new House();
        h.setId(1L);
        h.setPublisherId(7L);
        h.setAuditStatus(1);
        h.setRackStatus(0); // 上架在租：现在允许直接删除（房东完全处置权）
        when(houseMapper.selectById(1L)).thenReturn(h);

        service.deleteHouse(7L, 0, 1L);
        verify(houseMapper).deleteById(1L);
    }
}
