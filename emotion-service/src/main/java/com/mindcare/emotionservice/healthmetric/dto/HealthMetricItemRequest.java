package com.mindcare.emotionservice.healthmetric.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record HealthMetricItemRequest(
        @Size(max = 255)
        String externalSampleId,
        @NotBlank
        @Size(max = 50)
        String metricType,
        @NotNull
        BigDecimal value,
        @NotBlank
        @Size(max = 20)
        String unit,
        @NotNull
        OffsetDateTime recordedAt
) {
}
