package com.aliren.houserent.schedule;

import com.aliren.houserent.house.House;
import com.aliren.houserent.house.HouseMapper;
import com.aliren.houserent.robot.PushClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    private List<House> houses(int n) {
        List<House> list = new ArrayList<>();
        for (long i = 1; i <= n; i++) {
            list.add(house(i, "本周房源" + i, String.valueOf(i * 1000)));
        }
        return list;
    }

    @Test
    void weeklyPick_weekPlenty_picksTop3ThisWeek() {
        // 本周 ≥7 套：只查一次本周库（含 created_at 过滤），精选前 3
        when(houseMapper.selectList(any())).thenReturn(houses(7));
        when(houseMapper.selectCount(any())).thenReturn(7L);

        task.weeklyPick();

        ArgumentCaptor<QueryWrapper> qw = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(houseMapper, times(1)).selectList(qw.capture());
        assertThat(qw.getValue().getSqlSegment())
                .contains("audit_status").contains("rack_status").contains("created_at");

        ArgumentCaptor<String> md = ArgumentCaptor.forClass(String.class);
        verify(pushClient).sendGroupCard(anyString(), md.capture());
        // 前 3 套展示，第 4 套不在卡片里
        assertThat(md.getValue()).contains("本周房源1").contains("本周房源3")
                .doesNotContain("本周房源4");
        // 卡片带 dingtalk page/link 详情链接（无 pc_slide，与群卡片一致）
        assertThat(md.getValue()).contains(
                "👉 [查看房源详情](dingtalk://dingtalkclient/page/link?url=https%3A%2F%2Fh5.example.com%2F%23%2Flanding%3Fredirect%3D%252Fhouse%252F1)");
    }

    @Test
    void weeklyPick_fewThisWeek_fallsBackToLatestFive() {
        // 本周只有 2 套（<7）：先查本周候选，再回退查全站最新（第二次结果进卡片）
        List<House> weekHouses = houses(2);
        List<House> fallbackHouses = List.of(
                house(101L, "老房源A", "5100"),
                house(102L, "老房源B", "5200"));
        when(houseMapper.selectList(any()))
                .thenReturn(weekHouses)      // 本周候选查询
                .thenReturn(fallbackHouses); // 回退查询
        when(houseMapper.selectCount(any())).thenReturn(2L);

        task.weeklyPick();

        // 触发两次查询：本周 + 回退
        verify(houseMapper, times(2)).selectList(any());
        // 卡片展示的是回退查询的房源（证明走了回退分支）
        ArgumentCaptor<String> md = ArgumentCaptor.forClass(String.class);
        verify(pushClient).sendGroupCard(anyString(), md.capture());
        assertThat(md.getValue()).contains("老房源A").contains("老房源B")
                .doesNotContain("本周房源1");
    }

    @Test
    void weeklyPick_h5NotConfigured_noLinks() {
        task = new WeeklyPickTask(houseMapper, pushClient, "", "");
        when(houseMapper.selectList(any())).thenReturn(houses(1));
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
        when(houseMapper.selectList(any())).thenReturn(houses(1));
        when(houseMapper.selectCount(any())).thenReturn(1L);

        task.weeklyPick();

        ArgumentCaptor<String> md = ArgumentCaptor.forClass(String.class);
        verify(pushClient).sendGroupCard(anyString(), md.capture());
        assertThat(md.getValue()).contains("加入租房群").contains("joingroup");
    }

    @Test
    void weeklyPick_noGroupInviteUrl_noGroupSection() {
        // groupInviteUrl 为空时不显示入群入口
        when(houseMapper.selectList(any())).thenReturn(houses(1));
        when(houseMapper.selectCount(any())).thenReturn(1L);

        task.weeklyPick();

        ArgumentCaptor<String> md = ArgumentCaptor.forClass(String.class);
        verify(pushClient).sendGroupCard(anyString(), md.capture());
        assertThat(md.getValue()).doesNotContain("加入租房群");
    }
}
