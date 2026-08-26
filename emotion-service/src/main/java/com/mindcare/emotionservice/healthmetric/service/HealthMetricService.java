package com.mindcare.emotionservice.healthmetric.service;

import com.mindcare.emotionservice.healthmetric.dto.HealthMetricBatchRequest;
import com.mindcare.emotionservice.healthmetric.dto.HealthMetricBatchResponse;
import com.mindcare.emotionservice.healthmetric.dto.HealthMetricResponse;
import com.mindcare.emotionservice.healthmetric.dto.HealthMetricTrendPointResponse;
import com.mindcare.emotionservice.healthmetric.dto.HealthSourceSummaryResponse;
import com.mindcare.emotionservice.shared.dto.CursorPageResponse;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

public interface HealthMetricService {

    HealthMetricBatchResponse synchronizeMetrics(
            UUID userId,
            String idempotencyKey,
            HealthMetricBatchRequest request
    );

    CursorPageResponse<HealthMetricResponse> getMetrics(
            UUID userId,
            String metricType,
            OffsetDateTime from,
            OffsetDateTime to,
            String cursor,
            int limit
    );

    List<HealthMetricTrendPointResponse> getMetricTrends(
            UUID userId,
            String metricType,
            OffsetDateTime from,
            OffsetDateTime to,
            String bucket,
            ZoneId timezone
    );

    HealthSourceSummaryResponse getSourceSummary(UUID userId, String sourceType);

    HealthSourceSummaryResponse enableSourceSync(UUID userId, String sourceType);

    HealthSourceSummaryResponse revokeAndDeleteSourceData(UUID userId, String sourceType);
}
