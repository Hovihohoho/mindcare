package com.mindcare.emotionservice.healthmetric.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record HealthMetricItemRequest(
        @Size(max = 255)
        String externalSampleId,
        @NotBlank
        @Size(max = 50)
        String metricType,
        BigDecimal value,
        @Size(max = 20)
        String unit,
        OffsetDateTime recordedAt,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        @Size(max = 255)
        String sourceName,
        @Size(max = 255)
        String dataOrigin,
        OffsetDateTime sourceLastModifiedAt,
        Map<String, Object> details
) {
    public HealthMetricItemRequest(
            String externalSampleId,
            String metricType,
            BigDecimal value,
            String unit,
            OffsetDateTime recordedAt
    ) {
        this(externalSampleId, metricType, value, unit, recordedAt, null, null, null, null, null, null);
    }

    public HealthMetricItemRequest {
        details = details == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(details));
    }
}
