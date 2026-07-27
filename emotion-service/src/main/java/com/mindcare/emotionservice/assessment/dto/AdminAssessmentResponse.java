package com.mindcare.emotionservice.assessment.dto;

import com.mindcare.emotionservice.assessment.entity.AssessmentCode;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AdminAssessmentResponse(
        UUID id,
        AssessmentCode code,
        Integer assessmentVersion,
        String status,
        String title,
        String description,
        List<AdminQuestionResponse> questions,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public AdminAssessmentResponse {
        questions = List.copyOf(questions);
    }
}
