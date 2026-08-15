package com.aliren.rent.admin;

import com.aliren.core.common.BusinessException;
import com.aliren.rent.house.House;
import com.aliren.rent.house.HouseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class AdminAuditService {

    private final HouseMapper houseMapper;

    public AdminAuditService(HouseMapper houseMapper) {
        this.houseMapper = houseMapper;
    }

    /**
     * 审核房源：通过(1) / 驳回(2)
     *
     * @param operatorId   操作人 user.id
     * @param operatorRole 操作人角色（1=管理员）
     */
    @Transactional
    public void audit(Long operatorId, int operatorRole, Long houseId, boolean pass, String reason) {
        if (operatorRole != 1) {
            throw new BusinessException(403, "无权限：仅管理员可审核");
        }
        House h = requirePending(houseId);
        if (pass) {
            h.setAuditStatus(1);
            h.setAuditReason(null);
        } else {
            if (!StringUtils.hasText(reason)) {
                throw new BusinessException("驳回原因不能为空");
            }
            h.setAuditStatus(2);
            h.setAuditReason(reason);
        }
        h.setAuditorId(operatorId);
        h.setAuditTime(LocalDateTime.now());
        houseMapper.updateById(h);
        // TODO(后续任务): 审核通过后触发「机器人推卡片到子群 + 订阅批量匹配」
    }

    /**
     * 已租出下架：仅发布人本人或管理员可操作
     *
     * @param operatorId   操作人 user.id
     * @param operatorRole 操作人角色
     * @param houseId      房源 id
     */
    @Transactional
    public void offRack(Long operatorId, int operatorRole, Long houseId) {
        House h = houseMapper.selectById(houseId);
        if (h == null) {
            throw new BusinessException(404, "房源不存在");
        }
        boolean isOwner = h.getPublisherId().equals(operatorId);
        boolean isAdmin = operatorRole == 1;
        if (!isOwner && !isAdmin) {
            throw new BusinessException(403, "无权限：仅发布人或管理员可下架");
        }
        h.setRackStatus(1); // 已租出
        houseMapper.updateById(h);
    }

    private House requirePending(Long houseId) {
        House h = houseMapper.selectById(houseId);
        if (h == null || h.getAuditStatus() != 0) {
            throw new BusinessException(404, "房源不存在或不在待审核状态");
        }
        return h;
    }
}
