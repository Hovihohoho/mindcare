package com.mindcare.emotionservice.risk.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.math.BigDecimal;

public record RiskAlertResponse(
        UUID id,
        String alertLevel,
        String triggerReason,
        String ruleVersion,
        String reasonCode,
        UUID sourceResultId,
        Boolean notified,
        OffsetDateTime createdAt,
        String alertCategory,
        String benchmarkPolicyKey,
        String benchmarkPolicyVersion,
        String benchmarkSourceUrl,
        String metricType,
        BigDecimal observedValue,
        String observedUnit,
        String recommendedPlanTemplateCode
) {
    public RiskAlertResponse(UUID id, String alertLevel, String triggerReason, String ruleVersion,
                             String reasonCode, UUID sourceResultId, Boolean notified, OffsetDateTime createdAt) {
        this(id, alertLevel, triggerReason, ruleVersion, reasonCode, sourceResultId, notified, createdAt,
                null, null, null, null, null, null, null, null);
    }
}
