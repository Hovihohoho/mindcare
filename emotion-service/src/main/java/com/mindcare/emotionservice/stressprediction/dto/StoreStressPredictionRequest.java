package com.mindcare.emotionservice.stressprediction.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StoreStressPredictionRequest(
        @NotNull LocalDate featureDate,
        @NotBlank @Size(max = 80) String timezone,
        @NotBlank @Size(max = 60) String featureVersion,
        @Min(1) @Max(5) int stressScore,
        @NotBlank @Size(max = 30) String relativeLevel,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal confidence,
        @NotBlank @Size(max = 80) String modelVersion
) {
}
