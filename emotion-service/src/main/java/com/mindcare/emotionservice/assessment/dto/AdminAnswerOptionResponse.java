package com.mindcare.emotionservice.assessment.dto;

import java.util.UUID;

public record AdminAnswerOptionResponse(
        UUID id,
        String optionText,
        Integer scoreValue
) {
}
