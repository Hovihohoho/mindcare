package com.mindcare.ai_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(String apiKey, String baseUrl, String embeddingModel,
                               String chatModel, String fallbackChatModel,
                               int embeddingDimensions) {}
