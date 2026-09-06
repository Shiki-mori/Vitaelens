package com.phrolova.vitaelensbackend.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.phrolova.vitaelensbackend.common.ErrorCode;
import com.phrolova.vitaelensbackend.exception.BizException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AiClient {

    @Value("${ai.base-url}")
    private String baseUrl;

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.model}")
    private String model;

    @Value("${ai.timeout-seconds:60}")
    private int timeoutSeconds;

    @Value("${ai.max-tokens:8192}")
    private int maxTokens;

    /**
     * DeepSeek V4 默认开启思考模式。思考 token 计入 max_tokens，
     * 额度耗尽时会返回 HTTP 200 但 content 为空。
     */
    @Value("${ai.thinking-enabled:false}")
    private boolean thinkingEnabled;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private HttpClient httpClient;

    @PostConstruct
    void initHttpClient() {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }

    /**
     * 同步调用大模型，返回文本
     */
    public String chat(String systemPrompt, String userMessage) {
        String requestBody = buildRequestBody(systemPrompt, userMessage);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(normalizeBaseUrl(baseUrl) + "/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("AI 调用失败，status={}, bodyLength={}",
                        response.statusCode(),
                        response.body() == null ? 0 : response.body().length());
                throw new BizException(ErrorCode.AI_ERROR, "AI 分析服务暂时不可用");
            }

            String body = response.body();
            if (body == null || body.isBlank()) {
                log.error("AI 调用失败：响应体为空");
                throw new BizException(ErrorCode.AI_ERROR, "AI 分析服务暂时不可用");
            }

            JsonNode root = objectMapper.readTree(body);
            return extractMessageContent(root);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI 调用失败", e);
            throw new BizException(ErrorCode.AI_ERROR, "AI 分析服务异常，请稍后再试");
        }
    }

    /**
     * 同步调用大模型，要求返回 JSON 格式
     */
    public String chatJson(String systemPrompt, String userMessage) {
        String systemInstructions = systemPrompt + "\n\n你必须严格以 JSON 格式返回结果，不要包含任何其他文本或 markdown 标记。";
        return chat(systemInstructions, userMessage);
    }

    private String buildRequestBody(String systemPrompt, String userMessage) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userMessage)
            ));
            body.put("temperature", 0.3);
            body.put("max_tokens", maxTokens);
            // DeepSeek V4 思考模式默认开启；结构化 JSON 场景关闭，避免 reasoning 占满输出额度
            body.put("thinking", Map.of("type", thinkingEnabled ? "enabled" : "disabled"));
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "请求构建失败");
        }
    }

    static String normalizeBaseUrl(String rawBaseUrl) {
        if (rawBaseUrl == null || rawBaseUrl.isBlank()) {
            return "";
        }
        String trimmed = rawBaseUrl.trim();
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.endsWith("/v1")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed;
    }

    static String extractMessageContent(JsonNode root) {
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            log.error("AI 响应缺少 choices");
            throw new BizException(ErrorCode.AI_ERROR, "AI 未返回有效分析结果，请重试");
        }

        JsonNode choice = choices.get(0);
        JsonNode message = choice.path("message");
        String content = extractText(message.path("content"));
        if (content.isBlank()) {
            String finishReason = choice.path("finish_reason").asText("");
            boolean hasReasoning = !extractText(message.path("reasoning_content")).isBlank();
            log.error("AI 返回空 content, finishReason={}, hasReasoningContent={}",
                    finishReason, hasReasoning);
            throw new BizException(ErrorCode.AI_ERROR, "AI 未返回有效分析结果，请重试");
        }
        return normalizeJsonContent(content);
    }

    static String extractText(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isArray()) {
            StringBuilder builder = new StringBuilder();
            for (JsonNode part : node) {
                if (part.isTextual()) {
                    builder.append(part.asText());
                } else {
                    builder.append(part.path("text").asText(""));
                }
            }
            return builder.toString();
        }
        return "";
    }

    static String normalizeJsonContent(String content) {
        String trimmed = content.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        int firstNewline = trimmed.indexOf('\n');
        if (firstNewline >= 0) {
            trimmed = trimmed.substring(firstNewline + 1);
        }
        int fence = trimmed.lastIndexOf("```");
        if (fence >= 0) {
            trimmed = trimmed.substring(0, fence);
        }
        return trimmed.trim();
    }
}
