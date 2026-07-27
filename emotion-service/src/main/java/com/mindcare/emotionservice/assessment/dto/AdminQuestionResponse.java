package com.mindcare.emotionservice.assessment.dto;

import java.util.List;
import java.util.UUID;

public record AdminQuestionResponse(
        UUID id,
        String questionText,
        Integer orderIndex,
        List<AdminAnswerOptionResponse> answerOptions
) {
    public AdminQuestionResponse {
        answerOptions = List.copyOf(answerOptions);
    }
}
