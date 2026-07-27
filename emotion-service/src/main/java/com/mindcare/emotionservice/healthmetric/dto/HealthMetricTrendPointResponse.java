package com.mindcare.emotionservice.healthmetric.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record HealthMetricTrendPointResponse(
        OffsetDateTime periodStart,
        OffsetDateTime periodEnd,
        String metricType,
        BigDecimal averageValue,
        long count,
        String unit
) {
}
