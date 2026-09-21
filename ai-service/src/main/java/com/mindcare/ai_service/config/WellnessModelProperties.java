package com.mindcare.ai_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.wellness-model")
public record WellnessModelProperties(
        boolean enabled,
        String featureVersion,
        int expectedFeatures,
        String version,
        ModelArtifact sleepMinutes,
        ModelArtifact steps,
        ModelArtifact restingHeartRate
) {
    public record ModelArtifact(String path, String metadataPath, String modelVersion) {
    }
}
