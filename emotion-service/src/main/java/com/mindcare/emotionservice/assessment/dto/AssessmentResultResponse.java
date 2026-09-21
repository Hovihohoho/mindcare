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
        Integer normalizedScore,
        String interpretationLevel,
        String scoringPolicyKey,
        String scoringPolicyVersion,
        String benchmarkPolicyKey,
        String benchmarkPolicyVersion,
        List<RiskSignalResponse> riskSignals,
        String screeningNotice,
        List<String> recommendations,
        OffsetDateTime createdAt
) {
    public AssessmentResultResponse {
        recommendations = List.copyOf(recommendations);
        riskSignals = riskSignals == null ? List.of() : List.copyOf(riskSignals);
    }

    public AssessmentResultResponse(UUID resultId, AssessmentCode assessmentCode,
            Integer assessmentVersion, Integer totalScore, String riskLevel,
            String screeningNotice, List<String> recommendations, OffsetDateTime createdAt) {
        this(resultId, assessmentCode, assessmentVersion, totalScore, riskLevel, null, riskLevel,
                null, null, null, null, List.of(), screeningNotice, recommendations, createdAt);
    }

    public record RiskSignalResponse(String type, String reasonCode, Integer responseValue, String ruleVersion) {}
}
