package com.mindcare.emotionservice.healthmetric.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public record HealthSourceSummaryResponse(
        String sourceType,
        boolean syncEnabled,
        OffsetDateTime revokedAt,
        long recordCount,
        OffsetDateTime oldestRecordAt,
        OffsetDateTime newestRecordAt,
        Map<String, Long> recordsByMetricType
) {}
