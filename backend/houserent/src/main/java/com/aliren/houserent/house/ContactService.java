package com.aliren.houserent.house;

import com.aliren.core.common.BusinessException;
import com.aliren.core.user.User;
import com.aliren.core.user.UserMapper;
import com.aliren.houserent.house.dto.ContactResponse;
import com.aliren.houserent.pushlog.PushLogService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 联系房东（产品 4.1：不留手机号，钉钉内联系）：
 * 点击"钉钉内联系房东" → 返回房东钉钉身份（staffId），前端调 dd.biz.chat.openSingleChat
 * 直接唤起钉钉单聊窗口与房东沟通（无需机器人/工作通知）。
 */
@Service
public class ContactService {

    private final HouseMapper houseMapper;
    private final UserMapper userMapper;
    private final PushLogService pushLogService;

    public ContactService(HouseMapper houseMapper, UserMapper userMapper, PushLogService pushLogService) {
        this.houseMapper = houseMapper;
        this.userMapper = userMapper;
        this.pushLogService = pushLogService;
    }

    /** 获取房东钉钉身份（staffId）；记录联系动作留痕 */
    public ContactResponse contact(Long contactorId, Long houseId) {
        House h = houseMapper.selectById(houseId);
        if (h == null || h.getAuditStatus() != House.AUDIT_ONLINE) {
            throw new BusinessException(404, "房源不存在或未上架");
        }
        User owner = userMapper.selectById(h.getPublisherId());
        if (owner == null || !StringUtils.hasText(owner.getDingtalkUserId())) {
            throw new BusinessException(500, "房东暂未开通钉钉身份，请稍后再试");
        }
        pushLogService.record(contactorId, null, null,
                "发起联系房源：「" + h.getCommunity() + "」" + h.getHouseType() + "（房东 " + owner.getNickname() + "）");
        return new ContactResponse(owner.getNickname(), owner.getDingtalkUserId());
    }
}
