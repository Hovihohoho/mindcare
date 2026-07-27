package com.mindcare.bookingservice.shared.dto;

import java.util.List;

public record ProblemResponse(
        String type,
        String title,
        int status,
        String code,
        String detail,
        String traceId,
        List<FieldErrorResponse> fieldErrors) {
}
