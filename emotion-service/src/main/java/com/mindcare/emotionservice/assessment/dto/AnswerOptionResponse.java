package com.mindcare.emotionservice.assessment.dto;

import java.util.UUID;

public record AnswerOptionResponse(
        UUID id,
        String optionText
) {
}
