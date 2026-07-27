package com.mindcare.emotionservice.shared.dto;

public record FieldErrorResponse(
        String field,
        String code
) {
}
