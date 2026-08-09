package com.mindcare.emotionservice.assessment.dto;

import com.mindcare.emotionservice.assessment.entity.AssessmentCode;

import java.util.List;
import java.util.UUID;

public record AssessmentDetailResponse(
        UUID id,
        AssessmentCode code,
        Integer assessmentVersion,
        String title,
        String description,
        AssessmentEvidenceResponse evidence,
        List<QuestionResponse> questions
) {
    public AssessmentDetailResponse {
        questions = List.copyOf(questions);
    }

    public AssessmentDetailResponse(
            UUID id, AssessmentCode code, Integer assessmentVersion,
            String title, String description, List<QuestionResponse> questions) {
        this(id, code, assessmentVersion, title, description, null, questions);
    }
}
