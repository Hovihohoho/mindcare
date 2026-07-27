package com.mindcare.emotionservice.shared.dto;

import java.util.List;

public record ApiErrorResponse(
        String type,
        String title,
        int status,
        String code,
        String detail,
        String traceId,
        List<FieldErrorResponse> fieldErrors
) {
    public ApiErrorResponse {
        fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
    }
}
