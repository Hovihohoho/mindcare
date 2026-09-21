package com.mindcare.emotionservice.assessment.dto;

import com.mindcare.emotionservice.assessment.entity.AssessmentCode;

import java.util.UUID;

public record AssessmentSummaryResponse(
        UUID id,
        AssessmentCode code,
        Integer assessmentVersion,
        String title,
        String description,
        AssessmentEvidenceResponse evidence
) {
    public AssessmentSummaryResponse(
            UUID id, AssessmentCode code, Integer assessmentVersion,
            String title, String description) {
        this(id, code, assessmentVersion, title, description, null);
    }
}
