package com.mindcare.emotionservice.healthmetric.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DailyHealthEvaluationResponse(
        LocalDate evaluationDate,
        String timezone,
        String ruleVersion,
        int baselineWindowDays,
        boolean clinicalDiagnosis,
        String usageNotice,
        List<Signal> signals
) {
    public record Signal(
            String policyKey,
            String metricType,
            String status,
            List<String> reasonCodes,
            BigDecimal observedValue,
            String unit,
            int targetSampleCount,
            BigDecimal personalBaseline,
            int personalBaselineDays,
            BigDecimal absoluteChangeFromBaseline,
            BigDecimal percentageChangeFromBaseline,
            boolean generalThresholdTriggered,
            boolean personalDeviationTriggered,
            String generalReference,
            String message,
            String sourceTitle,
            String sourceUrl
    ) {
    }
}
