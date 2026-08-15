package com.aliren.houserent.match.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 通义千问（DashScope OpenAI 兼容模式）实现：配置 aliren.llm.api-key 后启用。
 * 与 MatchClientStub 互斥（@ConditionalOnProperty）。
 */
@Slf4j
@Component
@ConditionalOnExpression("'${aliren.llm.api-key:}' != ''")
public class QwenMatchClient implements MatchClient {

    private static final String CHAT_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

    private final String apiKey;
    private final String model;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QwenMatchClient(@Value("${aliren.llm.api-key}") String apiKey,
                           @Value("${aliren.llm.model:qwen-max}") String model,
                           @Value("${aliren.llm.timeout-seconds:10}") long timeoutSeconds) {
        this.apiKey = apiKey;
        this.model = model;
        this.timeout = Duration.ofSeconds(timeoutSeconds);
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    @Override
    public String complete(String prompt) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("temperature", 0.2);
            ArrayNode messages = body.putArray("messages");
            ObjectNode system = messages.addObject();
            system.put("role", "system");
            system.put("content", "你是一个严谨的租房匹配助手。只输出符合要求的 JSON，不要输出任何其他文字。");
            ObjectNode user = messages.addObject();
            user.put("role", "user");
            user.put("content", prompt);

            HttpRequest request = HttpRequest.newBuilder(URI.create(CHAT_URL))
                    .timeout(timeout)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").path(0).path("message").path("content").asText(null);
            if (content == null) {
                log.warn("qwen response missing content: {}", response.body());
                return null;
            }
            return content.trim();
        } catch (Exception e) {
            log.warn("qwen chat failed: {}", e.getMessage());
            return null;
        }
    }
}
