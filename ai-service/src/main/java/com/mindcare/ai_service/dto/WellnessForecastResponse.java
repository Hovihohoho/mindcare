package com.mindcare.ai_service.dto;

public record WellnessForecastResponse(
        long sleepMinutesNextDay,
        long stepsNextDay,
        double restingHeartRateNextDay,
        String modelVersion,
        boolean clinicalDiagnosis,
        String usageNotice
) {
}
