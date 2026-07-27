package com.mindcare.emotionservice.assessment.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssessmentAnswerRequest(
        @NotNull
        UUID questionId,
        @NotNull
        UUID optionId
) {
}
