package com.aliren.houserent.subscribe;

import com.aliren.core.common.BusinessException;
import com.aliren.houserent.pushlog.PushLog;
import com.aliren.houserent.pushlog.PushLogService;
import com.aliren.houserent.subscribe.dto.SubscribeCreateRequest;
import com.aliren.houserent.subscribe.dto.SubscribeResponse;
import com.aliren.houserent.subscribe.dto.SubscribeUpdateRequest;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 订阅：创建（自然语言）/ 我的订阅 / 修改（条件、免打扰、暂停恢复）/ 软删 / 推送历史。
 * LLM 解析结构化条件：创建订阅时由 SubscriptionParser 自动解析（LLM 或本地降级）。
 */
@Service
public class SubscribeService {

    private final SubscribeMapper subscribeMapper;
    private final PushLogService pushLogService;
    private final SubscriptionParser subscriptionParser;

    public SubscribeService(SubscribeMapper subscribeMapper, PushLogService pushLogService,
                           SubscriptionParser subscriptionParser) {
        this.subscribeMapper = subscribeMapper;
        this.pushLogService = pushLogService;
        this.subscriptionParser = subscriptionParser;
    }

    /** 创建订阅：初始状态活跃(0)，pushCount=0；自动解析结构化条件（请求自带则优先） */
    public Long create(Long userId, SubscribeCreateRequest req) {
        Subscribe s = new Subscribe();
        s.setUserId(userId);
        s.setType(req.getType());
        s.setRawText(req.getRawText());
        String condition = req.getStructuredCondition();
        if (!StringUtils.hasText(condition)) {
            condition = subscriptionParser.parse(req.getRawText());
        }
        s.setStructuredCondition(condition);
        s.setQuietHours(req.getQuietHours());
        s.setStatus(Subscribe.STATUS_ACTIVE);
        s.setPushCount(0);
        subscribeMapper.insert(s);
        return s.getId();
    }

    /** 我的订阅：不含已删除，按创建倒序 */
    public List<SubscribeResponse> list(Long userId) {
        QueryWrapper<Subscribe> qw = new QueryWrapper<>();
        qw.eq("user_id", userId)
                .ne("status", Subscribe.STATUS_DELETED)
                .orderByDesc("created_at");
        return subscribeMapper.selectList(qw).stream().map(SubscribeResponse::from).toList();
    }

    /** 修改订阅：仅本人；支持更新条件/原文/免打扰，以及暂停(1)/恢复(0) */
    @Transactional
    public SubscribeResponse update(Long userId, Long id, SubscribeUpdateRequest req) {
        Subscribe s = requireOwned(userId, id);
        if (StringUtils.hasText(req.getStructuredCondition())) {
            s.setStructuredCondition(req.getStructuredCondition());
        }
        if (StringUtils.hasText(req.getRawText())) {
            s.setRawText(req.getRawText());
        }
        if (req.getQuietHours() != null) {
            s.setQuietHours(req.getQuietHours());
        }
        if (req.getStatus() != null) {
            if (req.getStatus() != Subscribe.STATUS_ACTIVE && req.getStatus() != Subscribe.STATUS_PAUSED) {
                throw new BusinessException("状态不合法：仅支持活跃(0)/暂停(1)");
            }
            s.setStatus(req.getStatus());
        }
        subscribeMapper.updateById(s);
        return SubscribeResponse.from(s);
    }

    /** 删除订阅（软删）：仅本人 */
    @Transactional
    public void delete(Long userId, Long id) {
        Subscribe s = requireOwned(userId, id);
        s.setStatus(Subscribe.STATUS_DELETED);
        subscribeMapper.updateById(s);
    }

    /** 推送计数 +1（供 match/robot 调用），目标仅限订阅 owner 本人 */
    public void incrementPushCount(Long id) {
        Subscribe s = subscribeMapper.selectById(id);
        if (s == null) {
            return;
        }
        s.setPushCount(s.getPushCount() == null ? 1 : s.getPushCount() + 1);
        subscribeMapper.updateById(s);
    }

    /** 推送历史（仅本人）：订阅收到的匹配提醒记录 */
    public List<PushLog> listPushes(Long userId, Long id) {
        requireOwned(userId, id);
        return pushLogService.listBySubscribe(id);
    }

    private Subscribe requireOwned(Long userId, Long id) {
        Subscribe s = subscribeMapper.selectById(id);
        if (s == null || s.getStatus() == null || s.getStatus() == Subscribe.STATUS_DELETED) {
            throw new BusinessException(404, "订阅不存在");
        }
        if (!s.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权限：仅本人可操作订阅");
        }
        return s;
    }
}
