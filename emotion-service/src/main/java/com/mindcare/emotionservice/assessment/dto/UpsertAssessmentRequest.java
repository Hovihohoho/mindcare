package com.mindcare.emotionservice.assessment.dto;

import com.mindcare.emotionservice.assessment.entity.AssessmentCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpsertAssessmentRequest(
        @NotNull
        AssessmentCode code,
        @NotBlank
        @Size(max = 255)
        String title,
        String description,
        @NotEmpty
        List<@NotNull @Valid UpsertQuestionRequest> questions
) {
    public UpsertAssessmentRequest {
        questions = questions == null ? null : questions.stream().toList();
    }
}
