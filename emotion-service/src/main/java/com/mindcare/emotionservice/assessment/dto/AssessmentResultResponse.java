package com.mindcare.emotionservice.assessment.dto;

import com.mindcare.emotionservice.assessment.entity.AssessmentCode;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AssessmentResultResponse(
        UUID resultId,
        AssessmentCode assessmentCode,
        Integer assessmentVersion,
        Integer totalScore,
        String riskLevel,
        String screeningNotice,
        List<String> recommendations,
        OffsetDateTime createdAt
) {
    public AssessmentResultResponse {
        recommendations = List.copyOf(recommendations);
    }
}
