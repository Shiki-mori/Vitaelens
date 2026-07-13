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
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AiClient {

    // @Value注解：从配置文件 application.yml 中查找键为 ai.base-url 的值，将其注入到 baseUrl 变量中
    @Value("${ai.base-url}")
    private String baseUrl;

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.model}")
    private String model;

    @Value("${ai.timeout-seconds:60}")
    private int timeoutSeconds;

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
                .uri(URI.create(baseUrl + "/v1/chat/completions"))
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
                log.error("AI 调用失败，status={}, body={}", response.statusCode(), response.body());
                throw new BizException(ErrorCode.AI_ERROR, "AI 分析服务暂时不可用");
            }

            // 将HTTP响应的JSON字符串解析为树形节点
            JsonNode root = objectMapper.readTree(response.body());
            // 从根节点开始，逐层提取数据
            return root.path("choices")  // 获取"choices"数组
                    .get(0)                          // 获取数组第一个元素
                    .path("message")     // 获取"message"对象
                    .path("content")     // 获取”content“字段
                    .asText();
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
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userMessage)
                    ),
                    "temperature", 0.3,
                    "max_tokens", 4096
            );
            /*
              最终生成的 JSON 请求体示例：
               {
                   "model": "gpt-3.5",
                   "messages": [
                       {"role": "system", "content": "你是..."}.
                       {"role": "user", "content": "帮我解决..."}
                   ],
                   "temperature": 0.3,
                   "max_tokens": 4096
               }
             */
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "请求构建失败");
        }
    }
}
