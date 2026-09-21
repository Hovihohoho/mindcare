package com.mindcare.ai_service.dto;

public record StressPredictionResponse(
        int stressScore,
        String relativeLevel,
        int scaleMinimum,
        int scaleMaximum,
        float confidence,
        String modelVersion,
        String usageNotice
) {}
