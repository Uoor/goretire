package com.aliren.houserent.house;

import com.aliren.houserent.house.dto.RentReportResponse;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 租金周报：区域均价（在租已上架）+ 本周新上架/成交故事数据。
 * 数据来源：房源库挂牌记录（产品文档 4.6）。
 */
@Service
public class RentReportService {

    private final HouseMapper houseMapper;

    public RentReportService(HouseMapper houseMapper) {
        this.houseMapper = houseMapper;
    }

    /** 单区域统计（AI 定价参考用）：在租已上架房源的平均/最低/最高月租 */
    public RentReportResponse.RegionStat regionStat(String region) {
        if (region == null || region.isBlank()) {
            return null;
        }
        List<House> houses = houseMapper.selectList(new QueryWrapper<House>()
                .eq("audit_status", House.AUDIT_ONLINE)
                .eq("rack_status", House.RACK_RENTING)
                .eq("region", region));
        if (houses.isEmpty()) {
            return null;
        }
        RentReportResponse.RegionStat stat = new RentReportResponse.RegionStat();
        stat.setRegion(region);
        stat.setCount(houses.size());
        BigDecimal min = null;
        BigDecimal max = null;
        BigDecimal sum = BigDecimal.ZERO;
        for (House h : houses) {
            if (h.getRent() == null) {
                continue;
            }
            sum = sum.add(h.getRent());
            if (min == null || h.getRent().compareTo(min) < 0) min = h.getRent();
            if (max == null || h.getRent().compareTo(max) > 0) max = h.getRent();
        }
        stat.setAvgRent(sum.divide(BigDecimal.valueOf(houses.size()), 0, RoundingMode.HALF_UP));
        stat.setMinRent(min);
        stat.setMaxRent(max);
        return stat;
    }

    public RentReportResponse weekly() {
        LocalDate monday = LocalDate.now().with(java.time.DayOfWeek.MONDAY);

        List<House> online = houseMapper.selectList(new QueryWrapper<House>()
                .eq("audit_status", House.AUDIT_ONLINE)
                .eq("rack_status", House.RACK_RENTING));

        // 区域均价
        Map<String, List<BigDecimal>> byRegion = new LinkedHashMap<>();
        for (House h : online) {
            if (h.getRent() == null || h.getRegion() == null || h.getRegion().isBlank()) {
                continue;
            }
            byRegion.computeIfAbsent(h.getRegion(), k -> new ArrayList<>()).add(h.getRent());
        }
        List<RentReportResponse.RegionStat> regions = new ArrayList<>();
        byRegion.forEach((region, rents) -> {
            RentReportResponse.RegionStat stat = new RentReportResponse.RegionStat();
            stat.setRegion(region);
            stat.setCount(rents.size());
            stat.setAvgRent(rents.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(rents.size()), 0, RoundingMode.HALF_UP));
            regions.add(stat);
        });

        RentReportResponse resp = new RentReportResponse();
        resp.setRegions(regions);
        resp.setTotalOnline(online.size());
        resp.setWeekNew(houseMapper.selectCount(new QueryWrapper<House>()
                .ge("created_at", monday.atStartOfDay())));
        resp.setWeekRented(houseMapper.selectCount(new QueryWrapper<House>()
                .eq("feedback_answer", 1)
                .ge("updated_at", monday.atStartOfDay())));
        return resp;
    }
}
