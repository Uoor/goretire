package com.aliren.houserent.schedule;

import com.aliren.houserent.house.House;
import com.aliren.houserent.house.HouseMapper;
import com.aliren.houserent.robot.PushClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklyPickTaskTest {

    @Mock
    private HouseMapper houseMapper;
    @Mock
    private PushClient pushClient;

    private WeeklyPickTask task;

    @BeforeEach
    void setUp() {
        task = new WeeklyPickTask(houseMapper, pushClient, "https://h5.example.com", "");
    }

    private House house(Long id, String community, String rent) {
        House h = new House();
        h.setId(id);
        h.setCommunity(community);
        h.setHouseType("2室1厅");
        h.setArea(89);
        h.setRent(new BigDecimal(rent));
        h.setRegion("杭州西溪");
        h.setLabel(House.LABEL_DIRECT);
        h.setCommute("西溪园区 15 分钟");
        return h;
    }

    @Test
    void weeklyPick_pushesTop3Houses() {
        when(houseMapper.selectList(any())).thenReturn(List.of(house(1L, "西溪八方城", "5800")));
        when(houseMapper.selectCount(any())).thenReturn(3L);

        task.weeklyPick();

        ArgumentCaptor<String> title = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> md = ArgumentCaptor.forClass(String.class);
        verify(pushClient).sendGroupCard(title.capture(), md.capture());
        assertThat(title.getValue()).isEqualTo("本周精选");
        assertThat(md.getValue()).contains("西溪八方城").contains("5800").contains("房东直租");
        // 每条房源带 dingtalk page/link 详情链接（无 pc_slide，与群卡片一致）
        assertThat(md.getValue()).contains(
                "👉 [查看房源详情](dingtalk://dingtalkclient/page/link?url=https%3A%2F%2Fh5.example.com%2F%23%2Flanding%3Fredirect%3D%252Fhouse%252F1)");
    }

    @Test
    void weeklyPick_filtersThisWeekOnlineRenting() {
        when(houseMapper.selectList(any())).thenReturn(List.of());
        task.weeklyPick();

        // 精选列表限定：审核通过 + 在租中 + 本周（7 天内创建）
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.QueryWrapper> qw =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.query.QueryWrapper.class);
        verify(houseMapper).selectList(qw.capture());
        String sql = qw.getValue().getSqlSegment();
        assertThat(sql).contains("audit_status").contains("rack_status").contains("created_at");
        // 概览数同样限定本周 + 审核通过
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.QueryWrapper> qw2 =
                ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.query.QueryWrapper.class);
        verify(houseMapper).selectCount(qw2.capture());
        assertThat(qw2.getValue().getSqlSegment()).contains("audit_status").contains("created_at");
    }

    @Test
    void weeklyPick_h5NotConfigured_noLinks() {
        task = new WeeklyPickTask(houseMapper, pushClient, "", "");
        when(houseMapper.selectList(any())).thenReturn(List.of(house(1L, "西溪八方城", "5800")));
        when(houseMapper.selectCount(any())).thenReturn(1L);

        task.weeklyPick();

        ArgumentCaptor<String> md = ArgumentCaptor.forClass(String.class);
        verify(pushClient).sendGroupCard(anyString(), md.capture());
        // 未配置 H5 地址时不带跳转链接（纯文本信息也能看）
        assertThat(md.getValue()).doesNotContain("查看房源详情");
    }

    @Test
    void weeklyPick_noHouses_skips() {
        when(houseMapper.selectList(any())).thenReturn(List.of());
        task.weeklyPick();
        verify(pushClient, never()).sendGroupCard(anyString(), anyString());
    }

    @Test
    void weeklyPick_groupInviteUrl_included() {
        task = new WeeklyPickTask(houseMapper, pushClient, "https://h5.example.com",
                "https://qr.dingtalk.com/action/joingroup?code=test123");
        when(houseMapper.selectList(any())).thenReturn(List.of(house(1L, "西溪八方城", "5800")));
        when(houseMapper.selectCount(any())).thenReturn(1L);

        task.weeklyPick();

        ArgumentCaptor<String> md = ArgumentCaptor.forClass(String.class);
        verify(pushClient).sendGroupCard(anyString(), md.capture());
        assertThat(md.getValue()).contains("加入租房群").contains("joingroup");
    }

    @Test
    void weeklyPick_noGroupInviteUrl_noGroupSection() {
        // groupInviteUrl 为空时不显示入群入口
        when(houseMapper.selectList(any())).thenReturn(List.of(house(1L, "西溪八方城", "5800")));
        when(houseMapper.selectCount(any())).thenReturn(1L);

        task.weeklyPick();

        ArgumentCaptor<String> md = ArgumentCaptor.forClass(String.class);
        verify(pushClient).sendGroupCard(anyString(), md.capture());
        assertThat(md.getValue()).doesNotContain("加入租房群");
    }
}
