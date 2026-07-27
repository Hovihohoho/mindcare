package com.mindcare.emotionservice.assessment.dto;

import com.mindcare.emotionservice.assessment.entity.AssessmentCode;

import java.util.UUID;

public record AssessmentSummaryResponse(
        UUID id,
        AssessmentCode code,
        Integer assessmentVersion,
        String title,
        String description
) {
}
