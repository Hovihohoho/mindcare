package com.mindcare.emotionservice.assessment.dto;

import java.util.List;
import java.util.UUID;

public record QuestionResponse(
        UUID id,
        String questionText,
        Integer orderIndex,
        List<AnswerOptionResponse> answerOptions
) {
    public QuestionResponse {
        answerOptions = List.copyOf(answerOptions);
    }
}
