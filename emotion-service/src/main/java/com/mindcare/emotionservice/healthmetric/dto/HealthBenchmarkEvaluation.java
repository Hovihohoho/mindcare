package com.mindcare.emotionservice.healthmetric.dto;

import java.math.BigDecimal;

public record HealthBenchmarkEvaluation(
        String policyKey,
        String policyVersion,
        String metricType,
        String status,
        String reasonCode,
        BigDecimal observedValue,
        String unit,
        int observedDays,
        int windowDays,
        String message,
        String sourceTitle,
        String sourceUrl,
        String recommendedPlanTemplateCode
) {
    public boolean alertTriggered() {
        return !"WITHIN_BENCHMARK".equals(status) && !"INSUFFICIENT_DATA".equals(status);
    }
}
