package com.aliren.houserent.schedule;

import com.aliren.houserent.house.HouseMapper;
import com.aliren.houserent.robot.PushClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklyStoryTaskTest {

    @Mock
    private HouseMapper houseMapper;
    @Mock
    private PushClient pushClient;

    private WeeklyStoryTask task;

    @BeforeEach
    void setUp() {
        task = new WeeklyStoryTask(houseMapper, pushClient);
    }

    @Test
    void weeklyStory_countsFoundHomesAndPushes() {
        when(houseMapper.selectCount(any())).thenReturn(3L);

        task.weeklyStory();

        ArgumentCaptor<String> title = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> md = ArgumentCaptor.forClass(String.class);
        verify(pushClient).sendGroupCard(title.capture(), md.capture());
        assertThat(title.getValue()).isEqualTo("安居故事");
        assertThat(md.getValue()).contains("3");
    }
}
