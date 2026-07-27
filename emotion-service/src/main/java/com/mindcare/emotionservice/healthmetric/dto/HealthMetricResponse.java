package com.mindcare.emotionservice.healthmetric.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record HealthMetricResponse(
        UUID id,
        String metricType,
        BigDecimal value,
        String unit,
        String sourceType,
        OffsetDateTime recordedAt,
        OffsetDateTime createdAt
) {
}
