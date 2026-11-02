package com.aliren.recruit.web;

import com.aliren.core.common.ApiResponse;
import com.aliren.recruit.aitable.AitableClient;
import com.aliren.recruit.aitable.RecruitRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 招聘板块公开数据接口（/ali/jobs/ 静态页实时拉取）。
 * 免鉴权：WebConfig 对 /api/recruit/** 放行。
 */
@Slf4j
@RestController
@RequestMapping("/api/recruit")
public class RecruitJobsController {

    /** 页面展示与统计的最大取数条数（Notable v1.0 maxResults 上限 100） */
    private static final int FETCH_MAX = 100;
    /** 首页"最新岗位"卡片数 */
    private static final int LATEST_SHOWN = 8;
    /** 柱状图最多展示的分类/企业条数 */
    private static final int CHART_TOP = 6;

    private final AitableClient aitableClient;
    private final String baseId;
    private final String tableId;
    private final String viewId;
    private final String jobsListUrl;

    public RecruitJobsController(AitableClient aitableClient,
                                 @Value("${aliren.recruit.aitable.base-id:}") String baseId,
                                 @Value("${aliren.recruit.aitable.table-id:}") String tableId,
                                 @Value("${aliren.recruit.aitable.view-id:UuvnFan}") String viewId,
                                 @Value("${aliren.recruit.reply.jobs-list-url:}") String jobsListUrl) {
        this.aitableClient = aitableClient;
        this.baseId = baseId;
        this.tableId = tableId;
        this.viewId = viewId;
        this.jobsListUrl = jobsListUrl;
    }

    /** 岗位详情链接（与群卡片 recordUrl 同一格式）。 */
    private String recordUrl(RecruitRecord r) {
        if (baseId == null || baseId.isBlank() || tableId == null || tableId.isBlank()
                || r.recordId() == null || r.recordId().isBlank()) {
            return jobsListUrl;
        }
        return "https://alidocs.dingtalk.com/i/nodes/" + baseId
                + "?iframeQuery=record%3D" + tableId + "_" + viewId + "_" + r.recordId()
                + "&notable_standalone_record_redirect=true";
    }

    /**
     * GET /api/recruit/jobs
     * 返回：stats（岗位/企业/分类数）+ 分类、企业计数 + 最新岗位卡片。
     */
    @GetMapping("/jobs")
    public ApiResponse<Map<String, Object>> jobs() {
        List<RecruitRecord> all = new ArrayList<>(aitableClient.queryLatest(FETCH_MAX));
        // 急聘优先，其余按创建时间最新在前
        all.sort(Comparator
                .comparing((RecruitRecord r) -> r.priority() != null && r.priority().contains("急聘") ? 0 : 1)
                .thenComparing(RecruitRecord::createdAt, Comparator.nullsLast(Comparator.reverseOrder())));

        Map<String, Integer> categoryCount = new LinkedHashMap<>();
        Map<String, Integer> companyCount = new LinkedHashMap<>();
        for (RecruitRecord r : all) {
            List<String> cats = r.categories() == null ? List.of() : r.categories();
            if (cats.isEmpty()) {
                categoryCount.merge("其他", 1, Integer::sum);
            } else {
                cats.forEach(c -> categoryCount.merge(c, 1, Integer::sum));
            }
            String company = r.company() == null || r.company().isBlank() ? "未填写" : r.company();
            companyCount.merge(company, 1, Integer::sum);
        }
        List<Map<String, Object>> categories = topCounts(categoryCount);
        List<Map<String, Object>> companies = topCounts(companyCount);

        List<Map<String, Object>> jobs = new ArrayList<>();
        for (RecruitRecord r : all) {
            Map<String, Object> j = new LinkedHashMap<>();
            j.put("title", r.title());
            j.put("company", r.company());
            j.put("locations", r.locations() == null ? List.of() : r.locations());
            j.put("category", r.categories() == null || r.categories().isEmpty() ? "其他" : r.categories().get(0));
            j.put("urgent", r.priority() != null && r.priority().contains("急聘"));
            j.put("url", recordUrl(r));
            jobs.add(j);
            if (jobs.size() >= LATEST_SHOWN) break;
        }

        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("jobs", all.size());
        stats.put("companies", companyCount.size());
        stats.put("categories", categoryCount.size());
        data.put("stats", stats);
        data.put("categories", categories);
        data.put("companies", companies);
        data.put("jobs", jobs);
        return ApiResponse.ok(data);
    }

    /** 计数 Map → [{name,count}]，按数量降序，最多 CHART_TOP 条。 */
    private List<Map<String, Object>> topCounts(Map<String, Integer> count) {
        return count.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(CHART_TOP)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .toList();
    }
}
