package com.aliren.houserent.house;

import com.aliren.core.common.BusinessException;
import com.aliren.houserent.house.dto.HouseCreateRequest;
import com.aliren.houserent.house.dto.HouseListQuery;
import com.aliren.houserent.house.dto.HouseResponse;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class HouseService {

    private final HouseMapper houseMapper;

    public HouseService(HouseMapper houseMapper) {
        this.houseMapper = houseMapper;
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
        h.setLabel(req.getLabel());
        h.setPetOk(req.getPetOk() == null ? 0 : req.getPetOk());
        h.setCommute(req.getCommute());
        h.setImages(req.getImages());
        h.setDescription(req.getDescription());
        h.setAuditStatus(0);
        h.setRackStatus(0);
        h.setFeedbackAnswer(0);
        houseMapper.insert(h);
        return h.getId();
    }

    /** 列表：仅已上架(1)且在租(0)的房源，支持筛选 */
    public List<HouseResponse> list(HouseListQuery query) {
        QueryWrapper<House> qw = new QueryWrapper<>();
        qw.eq("audit_status", 1).eq("rack_status", 0);
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
        qw.orderByDesc("created_at");
        return houseMapper.selectList(qw).stream().map(HouseResponse::from).toList();
    }

    /** 详情：仅已上架可看（未上架/驳回对普通用户隐藏） */
    public HouseResponse detail(Long id) {
        House h = houseMapper.selectById(id);
        if (h == null || h.getAuditStatus() != 1) {
            throw new BusinessException(404, "房源不存在或未上架");
        }
        return HouseResponse.from(h);
    }
}
