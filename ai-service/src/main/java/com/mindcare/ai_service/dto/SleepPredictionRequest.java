package com.mindcare.ai_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record SleepPredictionRequest(
        @NotNull LocalDate date,
        @NotNull @Min(0) Integer totalSteps,
        @NotNull @Min(0) @Max(1440) Integer veryActiveMinutes,
        @NotNull @Min(0) @Max(1440) Integer fairlyActiveMinutes,
        @NotNull @Min(0) @Max(1440) Integer lightlyActiveMinutes,
        @NotNull @Min(0) @Max(1440) Integer sedentaryMinutes,
        @NotNull @Min(1) @Max(1440) Integer totalMinutesAsleep) {
}
