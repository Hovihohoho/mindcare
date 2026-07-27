package com.mindcare.emotionservice.healthmetric.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record HealthMetricBatchRequest(
        @NotBlank
        @Size(max = 50)
        String sourceType,
        @NotEmpty
        @Size(max = 100)
        List<@NotNull @Valid HealthMetricItemRequest> items
) {
    public HealthMetricBatchRequest {
        items = items == null ? null : items.stream().toList();
    }
}
