package com.mindcare.emotionservice.assessment.dto;

public record AssessmentEvidenceResponse(
        String publisher,
        String sourceTitle,
        String sourceUrl,
        Integer publicationYear,
        String instrumentVersion,
        String license,
        String scoringRuleVersion,
        String purpose,
        String limitation
) {
}
