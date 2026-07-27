package com.mindcare.emotionservice.assessment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record AssessmentSubmissionRequest(
        @NotNull
        @Positive
        Integer assessmentVersion,
        @NotEmpty
        List<@NotNull @Valid AssessmentAnswerRequest> answers
) {
    public AssessmentSubmissionRequest {
        answers = answers == null ? null : answers.stream().toList();
    }
}
