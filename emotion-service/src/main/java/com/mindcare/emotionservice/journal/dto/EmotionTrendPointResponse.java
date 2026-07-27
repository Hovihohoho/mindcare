package com.mindcare.emotionservice.journal.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record EmotionTrendPointResponse(
        OffsetDateTime periodStart,
        OffsetDateTime periodEnd,
        BigDecimal averageScore,
        long count,
        String mappingVersion
) {
}
