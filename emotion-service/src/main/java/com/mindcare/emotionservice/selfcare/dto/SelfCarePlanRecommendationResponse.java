package com.mindcare.emotionservice.selfcare.dto;

import java.math.BigDecimal;

public record SelfCarePlanRecommendationResponse(
        String templateCode,
        String reasonCode,
        String message,
        BigDecimal observedValue,
        String unit,
        String benchmarkPolicyKey,
        String benchmarkPolicyVersion,
        String benchmarkSourceUrl
) {}
