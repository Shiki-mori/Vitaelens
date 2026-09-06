package com.phrolova.vitaelensbackend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.phrolova.vitaelensbackend.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void extractMessageContent_readsPlainText() throws Exception {
        var root = objectMapper.readTree("""
                {"choices":[{"message":{"content":"{\\"overallScore\\":80}"},"finish_reason":"stop"}]}
                """);

        assertEquals("{\"overallScore\":80}", AiClient.extractMessageContent(root));
    }

    @Test
    void extractMessageContent_stripsMarkdownFence() throws Exception {
        var root = objectMapper.readTree("""
                {"choices":[{"message":{"content":"```json\\n{\\"overallScore\\":80}\\n```"},"finish_reason":"stop"}]}
                """);

        assertEquals("{\"overallScore\":80}", AiClient.extractMessageContent(root));
    }

    @Test
    void extractMessageContent_readsContentArray() throws Exception {
        var root = objectMapper.readTree("""
                {"choices":[{"message":{"content":[{"type":"text","text":"{\\"a\\":1}"}]},"finish_reason":"stop"}]}
                """);

        assertEquals("{\"a\":1}", AiClient.extractMessageContent(root));
    }

    @Test
    void extractMessageContent_emptyContentThrows() throws Exception {
        var root = objectMapper.readTree("""
                {"choices":[{"message":{"content":"","reasoning_content":"thinking..."},"finish_reason":"length"}]}
                """);

        BizException ex = assertThrows(BizException.class, () -> AiClient.extractMessageContent(root));
        assertEquals("AI 未返回有效分析结果，请重试", ex.getMessage());
    }

    @Test
    void normalizeBaseUrl_stripsTrailingV1() {
        assertEquals("https://api.deepseek.com", AiClient.normalizeBaseUrl("https://api.deepseek.com/v1/"));
        assertEquals("https://api.deepseek.com", AiClient.normalizeBaseUrl("https://api.deepseek.com"));
    }
}
