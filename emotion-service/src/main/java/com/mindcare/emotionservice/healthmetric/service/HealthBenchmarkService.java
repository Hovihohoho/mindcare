package com.mindcare.emotionservice.healthmetric.service;

import com.mindcare.emotionservice.healthmetric.dto.HealthBenchmarkEvaluation;
import com.mindcare.emotionservice.healthmetric.dto.DailyHealthEvaluationResponse;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

public interface HealthBenchmarkService {
    List<HealthBenchmarkEvaluation> evaluate(UUID userId);

    DailyHealthEvaluationResponse evaluateDay(UUID userId, LocalDate date, ZoneId timezone);
}
