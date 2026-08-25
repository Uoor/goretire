package com.aliren.recruit.aitable;

import com.aliren.core.common.BusinessException;
import com.aliren.recruit.dingtalk.DingTalkTokenClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 多维表真实查询：调用钉钉多维表 **Notable v1.0** 开放 API。
 *
 * 为什么用 v1.0：应用开通的 `Notable.Base.Read.All`（AI 表格应用读权限）覆盖的是 notable_1.0
 * 系列接口；v2.0 端点需要另一个 scope（Notable.Base.Read），实测一直报权限缺失。
 *
 * 调用方式（v1.0 实测可用）：
 * - token：应用级 access_token（gettoken，需应用开通 Notable.Base.Read.All）
 * - 端点：GET /v1.0/notable/bases/{baseId}/sheets/{tableId}/records
 * - 必填 query 参数：operatorId = **有表权限用户的 unionId**（v1.0 以该用户身份读取，传 userId 会报 paramError）
 * - 响应：records[].fields 按**字段名**键控（非字段 ID）
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "aliren.recruit", name = "enabled", havingValue = "true")
public class AitableDingTalkClient implements AitableClient {

    private static final String BASE_URL = "https://api.dingtalk.com/v1.0/notable/bases/";
    /** v1.0 接口 maxResults 上限 100 */
    private static final int FETCH_MAX = 100;
    private static final Duration TIMEOUT = Duration.ofSeconds(8);

    // 字段名（多维表「岗位信息」主表 8tveFG3 实测，v1.0 按字段名键控）
    private static final String F_TITLE = "职位名称";
    private static final String F_COMPANY = "公司";
    private static final String F_LOCATIONS = "工作地点";    // multipleSelect [{name,id}]
    private static final String F_DESC = "职位描述";
    private static final String F_REQ = "任职要求";
    private static final String F_SALARY = "薪资范围";
    private static final String F_CONTACTS = "联络人";       // user [{unionId,name}]
    private static final String F_PRIORITY = "优先级";       // singleSelect {name,id}
    private static final String F_STATUS = "岗位状态";       // singleSelect {name,id}
    private static final String F_CATEGORIES = "职类";       // multipleSelect [{name,id}]
    private static final String F_CREATED = "创建日期";      // date

    private final String appKey;
    private final String appSecret;
    private final String baseId;
    private final String tableId;
    /** 有表权限用户的 unionId（v1.0 接口必填操作者） */
    private final String operatorId;
    private final DingTalkTokenClient tokenClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    public AitableDingTalkClient(@Value("${aliren.recruit.aitable.app-key:}") String appKey,
                                 @Value("${aliren.recruit.aitable.app-secret:}") String appSecret,
                                 @Value("${aliren.recruit.aitable.base-id:}") String baseId,
                                 @Value("${aliren.recruit.aitable.table-id:}") String tableId,
                                 @Value("${aliren.recruit.aitable.operator-id:}") String operatorId,
                                 DingTalkTokenClient tokenClient) {
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.baseId = baseId;
        this.tableId = tableId;
        this.operatorId = operatorId;
        this.tokenClient = tokenClient;
    }

    @Override
    public List<RecruitRecord> query(String keyword) {
        if (baseId == null || baseId.isBlank() || tableId == null || tableId.isBlank()) {
            log.warn("[recruit-aitable] base-id/table-id 未配置，跳过查询");
            return List.of();
        }
        if (operatorId == null || operatorId.isBlank()) {
            log.warn("[recruit-aitable] operator-id(unionId) 未配置，跳过查询");
            return List.of();
        }
        String kw = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        try {
            JsonNode root = fetchRecords();
            List<RecruitRecord> all = new ArrayList<>();
            for (JsonNode rec : root.path("records")) {
                RecruitRecord r = parseRecord(rec.path("fields"));
                if (r != null) {
                    all.add(r);
                }
            }
            return all.stream()
                    .filter(RecruitRecord::published)
                    .filter(r -> kw.isEmpty() || match(r, kw))
                    .toList();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[recruit-aitable] 查询多维表异常", e);
            throw new BusinessException(500, "查询招聘岗位失败: " + e.getMessage());
        }
    }

    private boolean match(RecruitRecord r, String kw) {
        return contains(r.title(), kw) || contains(r.company(), kw)
                || r.locations().stream().anyMatch(l -> l.toLowerCase(Locale.ROOT).contains(kw))
                || r.categories().stream().anyMatch(c -> c.toLowerCase(Locale.ROOT).contains(kw))
                || contains(r.salary(), kw);
    }

    private boolean contains(String s, String kw) {
        return s != null && s.toLowerCase(Locale.ROOT).contains(kw);
    }

    private JsonNode fetchRecords() {
        String token = tokenClient.getAccessToken(appKey, appSecret);
        String url = BASE_URL + enc(baseId) + "/sheets/" + enc(tableId)
                + "/records?maxResults=" + FETCH_MAX + "&operatorId=" + enc(operatorId);
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("x-acs-dingtalk-access-token", token)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());
            if (json.hasNonNull("code") && !json.path("code").asText().isBlank()
                    && !"OK".equals(json.path("code").asText())) {
                throw new BusinessException(500, "多维表查询失败: " + json.path("code").asText()
                        + " " + json.path("message").asText());
            }
            return json;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[recruit-aitable] 读取多维表异常", e);
            throw new BusinessException(500, "读取多维表异常");
        }
    }

    private String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private RecruitRecord parseRecord(JsonNode fields) {
        try {
            return new RecruitRecord(
                    text(fields, F_TITLE),
                    text(fields, F_COMPANY),
                    names(fields, F_LOCATIONS),
                    text(fields, F_SALARY),
                    text(fields, F_DESC),
                    text(fields, F_REQ),
                    names(fields, F_CONTACTS),
                    text(fields, F_PRIORITY),
                    text(fields, F_STATUS),
                    names(fields, F_CATEGORIES),
                    text(fields, "热门标签"),
                    date(fields, F_CREATED)
            );
        } catch (Exception e) {
            log.warn("[recruit-aitable] 解析记录失败: {}", fields, e);
            return null;
        }
    }

    /** 文本字段：字符串或对象（如 singleSelect 的 {name}）取 name。 */
    private String text(JsonNode fields, String fieldName) {
        JsonNode v = fields.get(fieldName);
        if (v == null || v.isNull()) return null;
        if (v.isTextual()) return v.asText();
        if (v.isObject()) return v.path("name").asText(null);
        return v.asText(null);
    }

    /** 多选/用户字段：数组 [{name,...}] 取所有 name；也可能是单个字符串/对象。 */
    private List<String> names(JsonNode fields, String fieldName) {
        List<String> out = new ArrayList<>();
        JsonNode v = fields.get(fieldName);
        if (v == null || v.isNull()) return out;
        if (v.isArray()) {
            for (JsonNode item : v) {
                if (item.isTextual()) out.add(item.asText());
                else if (item.isObject() && item.hasNonNull("name")) out.add(item.path("name").asText());
            }
        } else if (v.isObject() && v.hasNonNull("name")) {
            out.add(v.path("name").asText());
        } else if (v.isTextual()) {
            out.add(v.asText());
        }
        return out;
    }

    private OffsetDateTime date(JsonNode fields, String fieldName) {
        String s = text(fields, fieldName);
        if (s == null || s.isBlank()) return null;
        try {
            return OffsetDateTime.parse(s);
        } catch (DateTimeParseException e) {
            // 兼容毫秒时间戳
            try {
                return Instant.ofEpochMilli(Long.parseLong(s)).atOffset(ZoneOffset.ofHours(8));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
    }
}
