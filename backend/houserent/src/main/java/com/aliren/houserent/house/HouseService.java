package com.aliren.houserent.house;

import com.aliren.core.common.BusinessException;
import com.aliren.core.user.User;
import com.aliren.core.user.UserMapper;
import com.aliren.houserent.house.dto.HouseCreateRequest;
import com.aliren.houserent.house.dto.HouseListQuery;
import com.aliren.houserent.house.dto.HouseResponse;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Service
public class HouseService {

    private final HouseMapper houseMapper;
    private final UserMapper userMapper;

    public HouseService(HouseMapper houseMapper, UserMapper userMapper) {
        this.houseMapper = houseMapper;
        this.userMapper = userMapper;
    }

    /** 发布房源：初始状态待审核(0) + 在租中(0) */
    public Long publish(Long publisherId, HouseCreateRequest req) {
        if (req.getRent() == null || req.getRent().signum() <= 0) {
            throw new BusinessException("租金不合法");
        }
        House h = new House();
        h.setPublisherId(publisherId);
        h.setCommunity(req.getCommunity());
        h.setRoomNo(req.getRoomNo());
        h.setRegion(req.getRegion());
        h.setHouseType(req.getHouseType());
        h.setArea(req.getArea());
        h.setRent(req.getRent());
        h.setDepositPay(req.getDepositPay());
        h.setLeaseTerm(req.getLeaseTerm());
        h.setLabel(req.getLabel());
        h.setPetOk(req.getPetOk() == null ? 0 : req.getPetOk());
        h.setCommute(req.getCommute());
        h.setImages(req.getImages());
        h.setDescription(req.getDescription());
        h.setAuditStatus(House.AUDIT_PENDING);
        h.setRackStatus(House.RACK_RENTING);
        h.setFeedbackAnswer(0);
        houseMapper.insert(h);
        return h.getId();
    }

    /** 列表：仅已上架(1)且在租(0)的房源，支持筛选 */
    public List<HouseResponse> list(HouseListQuery query) {
        QueryWrapper<House> qw = new QueryWrapper<>();
        qw.eq("audit_status", House.AUDIT_ONLINE).eq("rack_status", House.RACK_RENTING);
        if (StringUtils.hasText(query.getRegion())) {
            qw.eq("region", query.getRegion());
        }
        if (query.getMaxRent() != null) {
            qw.le("rent", query.getMaxRent());
        }
        if (query.getMinRent() != null) {
            qw.ge("rent", query.getMinRent());
        }
        if (query.getLabel() != null) {
            qw.eq("label", query.getLabel());
        }
        if (query.getPetOk() != null) {
            qw.eq("pet_ok", query.getPetOk());
        }
        if (StringUtils.hasText(query.getHouseType())) {
            qw.like("house_type", query.getHouseType());
        }
        if (Boolean.TRUE.equals(query.getNewOnly())) {
            qw.ge("created_at", LocalDate.now().minusDays(7).atStartOfDay());
        }
        qw.orderByDesc("created_at");
        return houseMapper.selectList(qw).stream().map(HouseResponse::from).toList();
    }

    /** 详情：仅已上架可看（未上架/驳回对普通用户隐藏），附发布人信息 */
    public HouseResponse detail(Long id) {
        House h = houseMapper.selectById(id);
        if (h == null || h.getAuditStatus() != House.AUDIT_ONLINE) {
            throw new BusinessException(404, "房源不存在或未上架");
        }
        HouseResponse r = HouseResponse.from(h);
        fillPublisher(r, h.getPublisherId());
        return r;
    }

    /** 我的发布：全部状态（含驳回原因/审核状态），供个人中心跟踪 */
    public List<HouseResponse> mine(Long userId) {
        QueryWrapper<House> qw = new QueryWrapper<>();
        qw.eq("publisher_id", userId).orderByDesc("created_at");
        return houseMapper.selectList(qw).stream().map(HouseResponse::from).toList();
    }

    /**
     * 轻问句回答（下架时选填，仅发布人）：
     * 0=跳过 1=找到新家 2=暂无；数据供「安居故事」周报统计。
     */
    public void feedback(Long userId, Long id, Integer answer) {
        if (answer == null || answer < 0 || answer > 2) {
            throw new BusinessException("轻问句回答不合法");
        }
        House h = houseMapper.selectById(id);
        if (h == null) {
            throw new BusinessException(404, "房源不存在");
        }
        if (!h.getPublisherId().equals(userId)) {
            throw new BusinessException(403, "无权限：仅发布人可操作");
        }
        h.setFeedbackAnswer(answer);
        houseMapper.updateById(h);
    }

    private void fillPublisher(HouseResponse r, Long publisherId) {
        if (publisherId == null) {
            return;
        }
        User u = userMapper.selectById(publisherId);
        if (u != null) {
            r.setPublisherName(u.getNickname());
            r.setPublisherAvatar(u.getAvatar());
        }
    }
}
