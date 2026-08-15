package com.aliren.houserent.report;

import com.aliren.core.common.BusinessException;
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
class ReportServiceTest {

    @Mock
    private ReportMapper reportMapper;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(reportMapper);
    }

    @Test
    void create_houseReport_pendingStatus() {
        when(reportMapper.insert(any(Report.class))).thenAnswer(inv -> {
            inv.getArgument(0, Report.class).setId(1L);
            return 1;
        });

        Long id = reportService.create(Report.TYPE_HOUSE, 5L, 7L, "信息不实");
        assertThat(id).isEqualTo(1L);

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportMapper).insert(captor.capture());
        Report saved = captor.getValue();
        assertThat(saved.getTargetId()).isEqualTo(5L);
        assertThat(saved.getReporterId()).isEqualTo(7L);
        assertThat(saved.getStatus()).isZero(); // 待处理
    }

    @Test
    void create_nonHouseType_rejected() {
        assertThatThrownBy(() -> reportService.create(Report.TYPE_USER, 5L, 7L, "原因"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("房源");
    }

    @Test
    void handle_missingResult_rejected() {
        // result 为空时在查库前即抛异常（无需 stubbing）
        assertThatThrownBy(() -> reportService.handle(2L, 1L, "  "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("处理结果");
    }

    @Test
    void handle_setsHandled() {
        Report r = new Report();
        r.setId(1L);
        r.setStatus(Report.STATUS_PENDING);
        when(reportMapper.selectById(1L)).thenReturn(r);

        reportService.handle(2L, 1L, "已核实，房源下架");
        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(Report.STATUS_HANDLED);
        assertThat(captor.getValue().getResult()).isEqualTo("已核实，房源下架");
    }

    @Test
    void list_filtersByStatus() {
        Report r = new Report();
        r.setId(1L);
        when(reportMapper.selectList(any())).thenReturn(List.of(r));
        assertThat(reportService.list(Report.STATUS_PENDING)).hasSize(1);
    }
}
