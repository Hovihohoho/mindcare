package com.mindcare.emotionservice.healthmetric.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record HealthBenchmarkSnapshot(
        List<DailySummary> dailySummaries,
        List<Observation> restingHeartRates,
        List<Observation> oxygenSaturations
) {
    public record DailySummary(LocalDate date, BigDecimal sleepHours, BigDecimal steps) {}
    public record Observation(OffsetDateTime recordedAt, BigDecimal value) {}
}
