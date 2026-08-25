package com.aliren.recruit.aitable;

import com.aliren.core.common.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 多维表真实查询：使用**用户授权 token**（而非应用 token）访问钉钉多维表开放 API。
 *
 * 背景：应用级 token 需要应用开通 {@code Notable.Base.Read} 且依赖钉钉权限同步，
 * 实测不稳定；改为由「有表权限的用户」一次性授权应用，拿到 refresh token 后
 * 模块自动刷新 user access token，用用户身份读表（用户对该多维表的权限即访问边界）。
 *
 * 端点：
 * - 刷新 token: POST /v1.0/oauth2/token（grantType=refresh_token，返回新 accessToken 与 refreshToken）
 * - 读记录:    GET  /v2.0/notable/bases/{baseId}/sheets/{tableId}/records
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "aliren.recruit", name = "enabled", havingValue = "true")
public class AitableDingTalkClient implements AitableClient {

    private static final String REFRESH_URL = "https://api.dingtalk.com/v1.0/oauth2/token";
    private static final String BASE_URL = "https://api.dingtalk.com/v2.0/notable/bases/";
    private static final int FETCH_MAX = 200;
    private static final Duration TIMEOUT = Duration.ofSeconds(8);
    /** user accessToken 官方有效期 7200s，提前 200s 过期 */
    private static final long TOKEN_TTL_MS = 7000_000L;

    // 字段 ID（多维表「岗位信息」主表 8tveFG3 实测）
    private static final String F_TITLE = "ZpHI0SH";       // 职位名称
    private static final String F_COMPANY = "rAtzOd7";     // 公司
    private static final String F_LOCATIONS = "12nmIsd";   // 工作地点 multipleSelect
    private static final String F_DESC = "rbxYVYg";        // 职位描述
    private static final String F_REQ = "yo3In3u";         // 任职要求
    private static final String F_SALARY = "xIRVH5q";      // 薪资范围
    private static final String F_CONTACTS = "e5Ouuxn";    // 联络人 user
    private static final String F_PRIORITY = "Jjt3KOX";    // 优先级 singleSelect
    private static final String F_STATUS = "glH0431";      // 岗位状态 singleSelect
    private static final String F_CATEGORIES = "7UqMnqs";  // 职类 multipleSelect
    private static final String F_TAGS = "GQ8AQsA";        // 热门标签 richText
    private static final String F_CREATED = "WNyq9MV";     // 创建日期 date

    private final String clientId;
    private final String clientSecret;
    private final String baseId;
    private final String tableId;
    /** 一次性授权得到的用户 refresh token（配置或 token 文件中） */
    private volatile String userRefreshToken;
    /** 可选：刷新后把轮换的新 refresh token 持久化到该文件，避免失配 */
    private final Path tokenFile;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();

    private volatile String cachedUserToken = "";
    private volatile long userTokenExpireAt = 0;

    public AitableDingTalkClient(@Value("${aliren.recruit.aitable.app-key:}") String clientId,
                                 @Value("${aliren.recruit.aitable.app-secret:}") String clientSecret,
                                 @Value("${aliren.recruit.aitable.base-id:}") String baseId,
                                 @Value("${aliren.recruit.aitable.table-id:}") String tableId,
                                 @Value("${aliren.recruit.aitable.user-refresh-token:}") String userRefreshToken,
                                 @Value("${aliren.recruit.aitable.user-token-file:}") String tokenFile) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.baseId = baseId;
        this.tableId = tableId;
        this.userRefreshToken = userRefreshToken;
        this.tokenFile = tokenFile == null || tokenFile.isBlank() ? null : Path.of(tokenFile);
        loadRefreshTokenFromFile();
    }

    @Override
    public List<RecruitRecord> query(String keyword) {
        if (baseId == null || baseId.isBlank() || tableId == null || tableId.isBlank()) {
            log.warn("[recruit-aitable] base-id/table-id 未配置，跳过查询");
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

    // ---- 用户 token 管理 ----

    private void loadRefreshTokenFromFile() {
        if (tokenFile == null || !Files.exists(tokenFile)) {
            return;
        }
        try {
            String saved = Files.readString(tokenFile).trim();
            if (!saved.isBlank()) {
                userRefreshToken = saved;
                log.info("[recruit-aitable] 从 token 文件加载 refresh token");
            }
        } catch (Exception e) {
            log.warn("[recruit-aitable] 读取 token 文件失败", e);
        }
    }

    private String getUserAccessToken() {
        long now = System.currentTimeMillis();
        if (!cachedUserToken.isEmpty() && now < userTokenExpireAt) {
            return cachedUserToken;
        }
        synchronized (this) {
            if (!cachedUserToken.isEmpty() && now < userTokenExpireAt) {
                return cachedUserToken;
            }
            if (userRefreshToken == null || userRefreshToken.isBlank()) {
                throw new BusinessException(500, "多维表用户 refresh token 未配置（需一次性授权）");
            }
            JsonNode json = refreshUserToken();
            cachedUserToken = json.path("accessToken").asText("");
            userTokenExpireAt = now + json.path("expireIn").asLong(7200) * 1000L - 200_000L;
            // refresh token 轮换：持久化新值，避免旧值失效后失配
            String newRefresh = json.path("refreshToken").asText("");
            if (!newRefresh.isBlank() && !newRefresh.equals(userRefreshToken)) {
                userRefreshToken = newRefresh;
                persistRefreshToken(newRefresh);
            }
            if (cachedUserToken.isEmpty()) {
                throw new BusinessException(500, "刷新用户 token 失败");
            }
            return cachedUserToken;
        }
    }

    /** POST /v1.0/oauth2/token（grantType=refresh_token） */
    private JsonNode refreshUserToken() {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("clientId", clientId);
            body.put("clientSecret", clientSecret);
            body.put("refreshToken", userRefreshToken);
            body.put("grantType", "refresh_token");
            HttpRequest request = HttpRequest.newBuilder(URI.create(REFRESH_URL))
                    .timeout(TIMEOUT)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());
            if (response.statusCode() >= 400 || !json.hasNonNull("accessToken")) {
                log.warn("[recruit-aitable] 刷新用户 token 失败: HTTP {} {}", response.statusCode(), response.body());
                throw new BusinessException(500, "刷新用户 token 失败，可能需重新授权");
            }
            return json;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[recruit-aitable] 刷新用户 token 异常", e);
            throw new BusinessException(500, "刷新用户 token 异常");
        }
    }

    private void persistRefreshToken(String token) {
        if (tokenFile == null) {
            return;
        }
        try {
            if (tokenFile.getParent() != null) {
                Files.createDirectories(tokenFile.getParent());
            }
            Files.writeString(tokenFile, token);
            log.info("[recruit-aitable] 新 refresh token 已持久化: {}", tokenFile);
        } catch (Exception e) {
            log.warn("[recruit-aitable] 持久化 refresh token 失败", e);
        }
    }

    // ---- 数据获取与解析 ----

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
        String token = getUserAccessToken();
        String url = BASE_URL + enc(baseId) + "/sheets/" + enc(tableId) + "/records?maxResults=" + FETCH_MAX;
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
                    userIds(fields, F_CONTACTS),
                    text(fields, F_PRIORITY),
                    text(fields, F_STATUS),
                    names(fields, F_CATEGORIES),
                    text(fields, F_TAGS),
                    date(fields, F_CREATED)
            );
        } catch (Exception e) {
            log.warn("[recruit-aitable] 解析记录失败: {}", fields, e);
            return null;
        }
    }

    private String text(JsonNode fields, String fieldId) {
        JsonNode v = fields.get(fieldId);
        if (v == null || v.isNull()) return null;
        if (v.isTextual()) return v.asText();
        if (v.isObject()) return v.path("name").asText(null);
        return v.asText(null);
    }

    private List<String> names(JsonNode fields, String fieldId) {
        List<String> out = new ArrayList<>();
        JsonNode v = fields.get(fieldId);
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

    private List<String> userIds(JsonNode fields, String fieldId) {
        List<String> out = new ArrayList<>();
        JsonNode v = fields.get(fieldId);
        if (v == null || v.isNull()) return out;
        if (v.isArray()) {
            for (JsonNode item : v) {
                if (item.isObject() && item.hasNonNull("userId")) out.add(item.path("userId").asText());
            }
        } else if (v.isObject() && v.hasNonNull("userId")) {
            out.add(v.path("userId").asText());
        }
        return out;
    }

    private OffsetDateTime date(JsonNode fields, String fieldId) {
        String s = text(fields, fieldId);
        if (s == null || s.isBlank()) return null;
        try {
            return OffsetDateTime.parse(s);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
