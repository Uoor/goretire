package com.aliren.houserent.pushlog;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 推送历史：订阅/求租匹配提醒落库，供"推送历史"查询（防骚扰透明化）。
 */
@Service
public class PushLogService {

    private final PushLogMapper pushLogMapper;

    public PushLogService(PushLogMapper pushLogMapper) {
        this.pushLogMapper = pushLogMapper;
    }

    public void record(Long userId, Long subscribeId, Long demandId, String content) {
        PushLog log = new PushLog();
        log.setUserId(userId);
        log.setSubscribeId(subscribeId);
        log.setDemandId(demandId);
        log.setContent(content);
        pushLogMapper.insert(log);
    }

    /** 某订阅的推送历史（按时间倒序） */
    public List<PushLog> listBySubscribe(Long subscribeId) {
        QueryWrapper<PushLog> qw = new QueryWrapper<>();
        qw.eq("subscribe_id", subscribeId).orderByDesc("created_at");
        return pushLogMapper.selectList(qw);
    }

    /** 某需求的推送历史（按时间倒序） */
    public List<PushLog> listByDemand(Long demandId) {
        QueryWrapper<PushLog> qw = new QueryWrapper<>();
        qw.eq("demand_id", demandId).orderByDesc("created_at");
        return pushLogMapper.selectList(qw);
    }
}
