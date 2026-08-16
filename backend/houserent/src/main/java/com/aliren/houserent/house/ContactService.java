package com.aliren.houserent.house;

import com.aliren.core.common.BusinessException;
import com.aliren.core.user.User;
import com.aliren.core.user.UserMapper;
import com.aliren.houserent.pushlog.PushLogService;
import com.aliren.houserent.robot.PushClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 联系房东（产品 4.1：不留手机号，钉钉内联系）：
 * 点击"钉钉内联系房东" → 通过工作通知转达房东，双方在钉钉内沟通。
 */
@Service
public class ContactService {

    private final HouseMapper houseMapper;
    private final UserMapper userMapper;
    private final PushClient pushClient;
    private final PushLogService pushLogService;

    public ContactService(HouseMapper houseMapper, UserMapper userMapper,
                          PushClient pushClient, PushLogService pushLogService) {
        this.houseMapper = houseMapper;
        this.userMapper = userMapper;
        this.pushClient = pushClient;
        this.pushLogService = pushLogService;
    }

    /** 通知房东"有校友感兴趣"；返回房东昵称（前端展示） */
    public String contact(Long contactorId, Long houseId) {
        House h = houseMapper.selectById(houseId);
        if (h == null || h.getAuditStatus() != House.AUDIT_ONLINE) {
            throw new BusinessException(404, "房源不存在或未上架");
        }
        User owner = userMapper.selectById(h.getPublisherId());
        if (owner == null || !StringUtils.hasText(owner.getDingtalkUserId())) {
            throw new BusinessException(500, "房东暂未开通钉钉通知，请稍后再试");
        }
        String content = "有校友对你的房源感兴趣：「" + h.getCommunity() + "」" + h.getHouseType()
                + " " + h.getArea() + "㎡ " + h.getRent() + "元/月（" + h.getRegion() + "），可在钉钉内与对方沟通。";
        pushClient.sendWorkNotice(owner.getDingtalkUserId(), content);
        pushLogService.record(h.getPublisherId(), null, null, content);
        return owner.getNickname();
    }
}
