package com.mindcare.emotionservice.healthmetric.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record HealthMetricResponse(
        UUID id,
        String metricType,
        BigDecimal value,
        String unit,
        String sourceType,
        String sourceName,
        String dataOrigin,
        OffsetDateTime recordedAt,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        OffsetDateTime sourceLastModifiedAt,
        Map<String, Object> details,
        OffsetDateTime createdAt
) {
}
