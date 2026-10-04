package com.mindcare.emotionservice.stressprediction.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StressPredictionRecordResponse(
        UUID id,
        LocalDate featureDate,
        String timezone,
        String featureVersion,
        int stressScore,
        String relativeLevel,
        BigDecimal confidence,
        String modelVersion,
        String alertLevel,
        boolean notificationRequired,
        OffsetDateTime notifiedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
