package com.mindcare.ai_service.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.mindcare.ai_service.config.GeminiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClientException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.beans.factory.annotation.Value;
import java.time.Duration;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class GeminiClient {
    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;
    @Value("${gemini.connect-timeout-seconds:3}") private int connectTimeoutSeconds = 3;
    @Value("${gemini.read-timeout-seconds:15}") private int readTimeoutSeconds = 15;

    public List<Double> embed(String text, String taskType) {
        ensureConfigured();
        Map<String, Object> body = Map.of(
                "content", Map.of("parts", List.of(Map.of("text", text))),
                "task_type", taskType,
                "output_dimensionality", properties.embeddingDimensions());
        JsonNode response = post("/models/" + properties.embeddingModel() + ":embedContent", body);
        JsonNode values = response.path("embedding").path("values");
        if (!values.isArray() || values.size() != properties.embeddingDimensions()) {
            throw new IllegalStateException("Gemini trả về vector embedding không hợp lệ");
        }
        return objectMapper.convertValue(values,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Double.class));
    }

    public String generate(String systemInstruction, String prompt) {
        ensureConfigured();
        try {
            return generateWithModel(properties.chatModel(), systemInstruction, prompt);
        } catch (IllegalStateException primaryFailure) {
            if (!StringUtils.hasText(properties.fallbackChatModel())
                    || properties.fallbackChatModel().equals(properties.chatModel())) {
                throw primaryFailure;
            }
            log.warn("Gemini model {} failed; retrying with {}", properties.chatModel(),
                    properties.fallbackChatModel());
            return generateWithModel(properties.fallbackChatModel(), systemInstruction, prompt);
        }
    }

    private String generateWithModel(String model, String systemInstruction, String prompt) {
        Map<String, Object> body = Map.of(
                "system_instruction", Map.of("parts", List.of(Map.of("text", systemInstruction))),
                "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of("maxOutputTokens", 1200));
        JsonNode response = post("/models/" + model + ":generateContent", body);
        JsonNode usage = response.path("usageMetadata");
        log.info("ai_provider_usage model={} prompt_tokens={} output_tokens={} total_tokens={}", model,
                usage.path("promptTokenCount").asInt(0), usage.path("candidatesTokenCount").asInt(0),
                usage.path("totalTokenCount").asInt(0));
        String text = response.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText();
        if (!StringUtils.hasText(text)) throw new IllegalStateException("Gemini không trả về nội dung");
        return text;
    }

    private JsonNode post(String path, Object body) {
        long started = System.nanoTime();
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(connectTimeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));
        try {
            JsonNode response = RestClient.builder().baseUrl(properties.baseUrl()).requestFactory(factory).build().post().uri(path)
                    .header("x-goog-api-key", properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(JsonNode.class);
            if (response == null) {
                log.warn("ai_provider_error operation={} kind=EmptyResponse", path);
                throw new IllegalStateException("Dịch vụ AI không trả về nội dung");
            }
            return response;
        } catch (RestClientResponseException exception) {
            log.warn("ai_provider_error operation={} status={}", path, exception.getStatusCode());
            throw new IllegalStateException("Gemini API lỗi: " + exception.getStatusCode(), exception);
        } catch (RestClientException exception) {
            log.warn("ai_provider_error operation={} kind={}", path, exception.getClass().getSimpleName());
            throw new IllegalStateException("Dịch vụ AI tạm thời không khả dụng", exception);
        } finally {
            log.info("ai_provider_latency operation={} duration_ms={}", path,
                    (System.nanoTime() - started) / 1_000_000);
        }
    }

    private void ensureConfigured() {
        if (!StringUtils.hasText(properties.apiKey())) {
            throw new IllegalStateException("Chưa cấu hình GEMINI_API_KEY");
        }
    }
}
