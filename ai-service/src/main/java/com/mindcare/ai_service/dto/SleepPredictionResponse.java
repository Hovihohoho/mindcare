package com.mindcare.ai_service.dto;

import java.time.LocalDate;

public record SleepPredictionResponse(
        LocalDate date, LocalDate targetDate,
        Double predictedSleepMinutes, String modelType) {
}
