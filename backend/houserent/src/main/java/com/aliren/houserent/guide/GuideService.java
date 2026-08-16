package com.aliren.houserent.guide;

import com.aliren.core.common.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 避坑指南（知识库）：直接对接钉钉知识库，钉钉是唯一数据源，H5 只读展示。
 * 树结构约定：根下每个「文件夹」= 一个板块（合同模板/押金避坑/骗局案例/区域攻略），
 * 文件夹内每篇文档 = 一条条目，正文即内容。
 *
 * 前置：应用需开通 Wiki.Workspace.Read / Wiki.Node.Read / Storage.File.Read 权限；
 * aliren.guide.workspace-id（知识库ID）与 aliren.guide.operator-id（管理员 unionId）配置后启用，
 * 未配置时接口返回空（前端可降级）。
 */
@Slf4j
@Service
public class GuideService {

    private static final String API_HOST = "https://api.dingtalk.com";
    private static final Duration TIMEOUT = Duration.ofSeconds(8);

    private final String appKey;
    private final String appSecret;
    private final String workspaceId;
    private final String operatorId;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GuideService(@Value("${aliren.dingtalk.app-key:}") String appKey,
                        @Value("${aliren.dingtalk.app-secret:}") String appSecret,
                        @Value("${aliren.guide.workspace-id:}") String workspaceId,
                        @Value("${aliren.guide.operator-id:}") String operatorId) {
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.workspaceId = workspaceId == null ? "" : workspaceId.trim();
        this.operatorId = operatorId == null ? "" : operatorId.trim();
        this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    }

    /** 是否已配置（未配置时前端可显示站内静态版或空态） */
    public boolean enabled() {
        return !workspaceId.isEmpty() && !operatorId.isEmpty();
    }

    /** 板块列表（根下文件夹；文档/文件（带扩展名如 .adoc）不是板块，跳过） */
    public List<GuideSection> sections() {
        JsonNode root = getJson("/v2.0/wiki/nodes?operatorId=" + operatorId
                + "&spaceId=" + workspaceId + "&parentNodeId=" + rootNodeId());
        List<GuideSection> out = new ArrayList<>();
        for (JsonNode n : root.path("nodes")) {
            String name = n.path("name").asText("");
            String nodeId = n.path("nodeId").asText("");
            if (name.isBlank() || nodeId.isBlank()) continue;
            // 文件夹无扩展名；文档/文件带扩展名（.adoc 等）→ 不作为板块
            if (name.contains(".")) continue;
            out.add(new GuideSection(nodeId, name, n.path("icon").asText("")));
        }
        return out;
    }

    /** 某板块下的条目（文件夹内文档） */
    public List<GuideItem> items(String sectionNodeId) {
        JsonNode nodes = getJson("/v2.0/wiki/nodes?operatorId=" + operatorId
                + "&spaceId=" + workspaceId + "&parentNodeId=" + sectionNodeId);
        List<GuideItem> out = new ArrayList<>();
        for (JsonNode n : nodes.path("nodes")) {
            String name = n.path("name").asText("").replace(".adoc", "");
            String nodeId = n.path("nodeId").asText("");
            if (name.isBlank() || nodeId.isBlank()) continue;
            GuideItem item = new GuideItem(nodeId, name);
            item.setUrl(n.path("url").asText(""));
            out.add(item);
        }
        return out;
    }

    /** 条目正文（从 blocks 结构拼纯文本；nodeId = 节点列表返回的 nodeId） */
    public String content(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            throw new BusinessException("文档参数缺失");
        }
        JsonNode json = getJson("/v1.0/doc/suites/documents/" + nodeId + "/blocks?operatorId=" + operatorId);
        JsonNode data = json.path("result").path("data");
        if (!data.isArray() || data.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (JsonNode b : data) {
            String text = b.path("heading").path("text").asText("");
            if (text.isBlank()) text = b.path("paragraph").path("text").asText("");
            if (!text.isBlank()) sb.append(text).append("\n\n");
        }
        return sb.toString().trim();
    }

    /** 根节点 id：从 workspace 列表按 URL 匹配（URL 中的 /i/spaces/{shortId} 是稳定标识） */
    /** 知识库访问 URL（钉钉内打开，供"去知识库下载模板"引导） */
    public String spaceUrl() {
        return findWorkspace().path("url").asText("");
    }

    private JsonNode findWorkspace() {
        JsonNode list = getJson("/v2.0/wiki/workspaces?operatorId=" + operatorId + "&maxResults=50");
        String shortId = workspaceId.contains("/")
                ? workspaceId.substring(workspaceId.lastIndexOf('/') + 1)
                : workspaceId;
        for (JsonNode w : list.path("workspaces")) {
            String url = w.path("url").asText("");
            if (url.contains("/i/spaces/" + shortId + "/") || url.endsWith("/i/spaces/" + shortId)) {
                return w;
            }
            if (workspaceId.equals(w.path("workspaceId").asText(""))) {
                return w;
            }
        }
        throw new BusinessException(500, "知识库未找到: " + workspaceId);
    }

    private String rootNodeId() {
        return findWorkspace().path("rootNodeId").asText("");
    }

    private JsonNode getJson(String path) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(API_HOST + path))
                    .timeout(TIMEOUT)
                    .header("x-acs-dingtalk-access-token", accessToken())
                    .GET().build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(resp.body());
            if (json.hasNonNull("code") && !json.path("code").asText("").isEmpty()
                    && !"0".equals(json.path("code").asText())) {
                log.warn("[guide] API 错误 {}: {}", json.path("code"), json.path("message"));
                throw new BusinessException(500, "知识库读取失败");
            }
            return json;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[guide] 请求异常", e);
            throw new BusinessException(500, "知识库读取失败");
        }
    }

    private String accessToken() {
        try {
            URI uri = URI.create("https://oapi.dingtalk.com/gettoken?appkey=" + appKey + "&appsecret=" + appSecret);
            HttpRequest req = HttpRequest.newBuilder(uri).timeout(TIMEOUT).GET().build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(resp.body());
            if (json.path("errcode").asInt(-1) != 0) {
                throw new BusinessException(500, "获取钉钉token失败");
            }
            return json.path("access_token").asText();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[guide] gettoken 异常", e);
            throw new BusinessException(500, "获取钉钉token异常");
        }
    }

    // ---------- DTO ----------

    public static class GuideSection {
        public String nodeId;
        public String name;
        public String icon;

        public GuideSection(String nodeId, String name, String icon) {
            this.nodeId = nodeId;
            this.name = name;
            this.icon = icon;
        }
    }

    public static class GuideItem {
        public String nodeId;
        public String name;
        public String url;

        public GuideItem(String nodeId, String name) {
            this.nodeId = nodeId;
            this.name = name;
        }

        public void setUrl(String url) { this.url = url; }
    }
}
