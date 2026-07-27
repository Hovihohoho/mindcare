package com.mindcare.emotionservice.healthmetric.dto;

import java.util.List;
import java.util.UUID;

public record HealthMetricBatchResponse(
        int acceptedCount,
        int duplicateCount,
        List<UUID> processedMetricIds
) {
    public HealthMetricBatchResponse {
        processedMetricIds = List.copyOf(processedMetricIds);
    }
}
