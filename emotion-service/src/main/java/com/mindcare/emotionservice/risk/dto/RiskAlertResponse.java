package com.mindcare.emotionservice.risk.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RiskAlertResponse(
        UUID id,
        String alertLevel,
        String triggerReason,
        Boolean notified,
        OffsetDateTime createdAt
) {
}
