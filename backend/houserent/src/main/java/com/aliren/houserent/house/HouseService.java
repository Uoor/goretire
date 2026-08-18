package com.aliren.houserent.house;

import com.aliren.core.common.BusinessException;
import com.aliren.core.user.User;
import com.aliren.core.user.UserMapper;
import com.aliren.houserent.house.dto.HouseCreateRequest;
import com.aliren.houserent.house.dto.HouseListQuery;
import com.aliren.houserent.house.dto.HouseResponse;
import com.aliren.houserent.house.dto.PageDto;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class HouseService {

    /** 分页默认大小 */
    private static final int DEFAULT_PAGE_SIZE = 20;
    /** 分页最大大小 */
    private static final int MAX_PAGE_SIZE = 100;

    private final HouseMapper houseMapper;
    private final UserMapper userMapper;

    public HouseService(HouseMapper houseMapper, UserMapper userMapper) {
        this.houseMapper = houseMapper;
        this.userMapper = userMapper;
    }

    /** 发布房源：初始状态待审核(0) + 在租中(0) */
    public Long publish(Long publisherId, HouseCreateRequest req) {
        log.info("publishing house: publisherId={}, community={}", publisherId, req.getCommunity());
        if (req.getRent() == null || req.getRent().signum() <= 0) {
            throw new BusinessException("租金不合法");
        }
        House h = new House();
        h.setPublisherId(publisherId);
        applyFields(h, req);
        h.setAuditStatus(House.AUDIT_PENDING);
        h.setRackStatus(House.RACK_RENTING);
        h.setFeedbackAnswer(0);
        houseMapper.insert(h);
        log.info("house published: id={}, publisherId={}", h.getId(), publisherId);
        return h.getId();
    }

    /**
     * 编辑房源（仅发布人本人）：更新字段并重新置为待审核（驳回后修改重提 / 上架后改内容）。
     * 保留发布人、房号；审核状态回待审核，需管理员重新审核。
     */
    public void update(Long userId, Long id, HouseCreateRequest req) {
        log.info("updating house: userId={}, houseId={}", userId, id);
        if (req.getRent() == null || req.getRent().signum() <= 0) {
            throw new BusinessException("租金不合法");
        }
        House h = houseMapper.selectById(id);
        if (h == null) {
            throw new BusinessException(404, "房源不存在");
        }
        if (!h.getPublisherId().equals(userId)) {
            throw new BusinessException(403, "无权限：仅发布人可修改");
        }
        applyFields(h, req);
        h.setAuditStatus(House.AUDIT_PENDING); // 修改后重新送审
        houseMapper.updateById(h);
        log.info("house updated: id={}, userId={}", id, userId);
    }

    private void applyFields(House h, HouseCreateRequest req) {
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
        h.setUtilities(req.getUtilities());
        h.setImages(req.getImages());
        h.setDescription(req.getDescription());
    }

    /** 列表：仅已上架(1)且在租(0)的房源，支持筛选 + 分页 */
    public PageDto<HouseResponse> list(HouseListQuery query) {
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
            // 户型语义归一化：把"一居/两居/三居"等口语说法映射到可 LIKE 命中的关键词，
            // 保证选"两居"能同时命中"2室1厅"与"两居"两种写法（LIKE 多条件 OR）
            List<String> keywords = houseTypeKeywords(query.getHouseType());
            qw.and(w -> {
                for (int i = 0; i < keywords.size(); i++) {
                    if (i == 0) {
                        w.like("house_type", keywords.get(i));
                    } else {
                        w.or().like("house_type", keywords.get(i));
                    }
                }
            });
        }
        if (Boolean.TRUE.equals(query.getNewOnly())) {
            qw.ge("created_at", LocalDate.now().minusDays(7).atStartOfDay());
        }
        qw.orderByDesc("created_at");
        long page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        long size = query.getSize() == null || query.getSize() < 1 ? DEFAULT_PAGE_SIZE : Math.min(query.getSize(), MAX_PAGE_SIZE);
        Page<House> p = houseMapper.selectPage(new Page<>(page, size), qw);
        List<HouseResponse> list = p.getRecords().stream().map(HouseResponse::from).toList();
        return new PageDto<>(p.getTotal(), list);
    }

    /** 户型关键词归一化：口语说法 → 一组可 LIKE 的关键词 */
    private List<String> houseTypeKeywords(String raw) {
        List<String> out = new ArrayList<>();
        String r = raw.trim();
        out.add(r);
        // 中文数字户型："一居/两居/三居…" → 同时匹配 "1室…/2室…/3室…"
        if (r.contains("居") && r.length() <= 3) {
            char n = r.charAt(0);
            String digit = switch (n) {
                case '一' -> "1";
                case '两', '二' -> "2";
                case '三' -> "3";
                case '四' -> "4";
                case '五' -> "5";
                default -> null;
            };
            if (digit != null) {
                out.add(digit + "室");
            }
        }
        // 整租：同时命中"室"（如 2室1厅）；合租保持"合租"关键词
        if (r.contains("整租")) {
            out.add("室");
        }
        return out;
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
        return houseMapper.selectList(qw).stream().map(h -> {
            HouseResponse r = HouseResponse.from(h);
            r.setRoomNo(h.getRoomNo()); // 仅本人可见
            r.setAuditReason(h.getAuditReason()); // 驳回原因仅本人可见，供修改重提
            return r;
        }).toList();
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
