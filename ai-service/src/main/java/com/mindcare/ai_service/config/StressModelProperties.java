package com.mindcare.ai_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.stress-model")
public record StressModelProperties(
        boolean enabled,
        String path,
        String version,
        int expectedFeatures
) {}
