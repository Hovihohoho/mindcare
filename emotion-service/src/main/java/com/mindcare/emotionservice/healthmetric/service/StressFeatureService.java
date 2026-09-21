package com.mindcare.emotionservice.healthmetric.service;

import com.mindcare.emotionservice.healthmetric.dto.StressFeatureResponse;
import com.mindcare.emotionservice.healthmetric.entity.HealthMetricEntity;
import com.mindcare.emotionservice.healthmetric.repository.HealthMetricRepository;
import com.mindcare.emotionservice.shared.exception.InvalidRequestException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StressFeatureService {
    static final String FEATURE_VERSION = "pmdata-features-v2";
    public static final List<String> FEATURE_NAMES = List.of(
            "hr_mean", "hr_std", "hr_count", "steps_total", "sleep_minutes",
            "sleep_efficiency", "sleep_deep_minutes", "sleep_rem_minutes",
            "sleep_awake_minutes", "resting_hr",
            "hr_mean_mean_3d", "hr_mean_mean_7d", "hr_mean_delta_7d",
            "steps_total_mean_3d", "steps_total_mean_7d", "steps_total_delta_7d",
            "sleep_minutes_mean_3d", "sleep_minutes_mean_7d", "sleep_minutes_delta_7d",
            "sleep_efficiency_mean_3d", "sleep_efficiency_mean_7d", "sleep_efficiency_delta_7d",
            "resting_hr_mean_3d", "resting_hr_mean_7d", "resting_hr_delta_7d");
    private static final List<String> TREND_SOURCES = List.of(
            "hr_mean", "steps_total", "sleep_minutes", "sleep_efficiency", "resting_hr");

    private final HealthMetricRepository repository;
    private final Clock clock;

    public StressFeatureService(HealthMetricRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public StressFeatureResponse build(UUID userId, LocalDate requestedDate, ZoneId timezone) {
        if (userId == null) {
            throw new InvalidRequestException("INVALID_USER_ID", "userId is required");
        }
        if (timezone == null) {
            throw new InvalidRequestException("INVALID_TIMEZONE", "timezone is required");
        }
        LocalDate today = OffsetDateTime.now(clock).atZoneSameInstant(timezone).toLocalDate();
        LocalDate featureDate = requestedDate == null ? today.minusDays(1) : requestedDate;
        if (featureDate.isAfter(today)) {
            throw new InvalidRequestException("INVALID_FEATURE_DATE", "feature date cannot be in the future");
        }

        LocalDate firstDate = featureDate.minusDays(7);
        OffsetDateTime from = firstDate.atStartOfDay(timezone).toOffsetDateTime();
        OffsetDateTime to = featureDate.plusDays(1).atStartOfDay(timezone).toOffsetDateTime();
        Map<LocalDate, DailyMetrics> daily = new LinkedHashMap<>();
        for (int offset = 7; offset >= 0; offset--) {
            daily.put(featureDate.minusDays(offset), new DailyMetrics());
        }

        load(userId, "HEART_RATE", from, to).forEach(metric -> addHeartRate(daily, metric, timezone));
        load(userId, "STEP_COUNT", from, to).forEach(metric -> addSteps(daily, metric, timezone));
        load(userId, "SLEEP_SESSION", from, to).forEach(metric -> addSleepSession(daily, metric, timezone));
        load(userId, "SLEEP_HOURS", from, to).forEach(metric -> addSleepHours(daily, metric, timezone));

        Map<LocalDate, Map<String, Double>> valuesByDate = new LinkedHashMap<>();
        daily.forEach((date, metrics) -> valuesByDate.put(date, metrics.values()));
        Map<String, Double> current = valuesByDate.get(featureDate);
        List<Double> features = new ArrayList<>(25);
        FEATURE_NAMES.subList(0, 10).forEach(name -> features.add(current.get(name)));
        for (String source : TREND_SOURCES) {
            Double mean3 = historyMean(valuesByDate, featureDate, source, 3, 2);
            Double mean7 = historyMean(valuesByDate, featureDate, source, 7, 3);
            features.add(mean3);
            features.add(mean7);
            features.add(current.get(source) == null || mean7 == null ? null : current.get(source) - mean7);
        }
        int availableBaseFeatures = (int) features.subList(0, 10).stream()
                .filter(value -> value != null && Double.isFinite(value))
                .count();
        return new StressFeatureResponse(
                FEATURE_VERSION,
                featureDate,
                timezone.getId(),
                FEATURE_NAMES,
                Collections.unmodifiableList(new ArrayList<>(features)),
                availableBaseFeatures);
    }

    private List<HealthMetricEntity> load(
            UUID userId,
            String type,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        return repository
                .findByUserIdAndMetricTypeAndDeletedAtIsNullAndRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderByRecordedAtAsc(
                        userId, type, from, to);
    }

    private void addHeartRate(
            Map<LocalDate, DailyMetrics> daily,
            HealthMetricEntity metric,
            ZoneId timezone
    ) {
        DailyMetrics values = daily.get(localDate(metric, timezone));
        if (values == null || metric.getMetricValue() == null) {
            return;
        }
        double heartRate = metric.getMetricValue().doubleValue();
        if (!Double.isFinite(heartRate)) {
            return;
        }
        values.heartRates.add(heartRate);
        if (isResting(metric.getDetails())) {
            values.restingHeartRates.add(heartRate);
        }
    }

    private void addSteps(
            Map<LocalDate, DailyMetrics> daily,
            HealthMetricEntity metric,
            ZoneId timezone
    ) {
        DailyMetrics values = daily.get(localDate(metric, timezone));
        if (values != null && metric.getMetricValue() != null) {
            values.stepsTotal = (values.stepsTotal == null ? 0.0 : values.stepsTotal)
                    + metric.getMetricValue().doubleValue();
        }
    }

    private void addSleepSession(
            Map<LocalDate, DailyMetrics> daily,
            HealthMetricEntity metric,
            ZoneId timezone
    ) {
        DailyMetrics values = daily.get(localDate(metric, timezone));
        if (values == null || metric.getStartTime() == null || metric.getEndTime() == null) {
            return;
        }
        double durationMinutes = Duration.between(metric.getStartTime(), metric.getEndTime()).toMinutes();
        if (durationMinutes <= 0 || durationMinutes <= values.sleepSessionMinutes) {
            return;
        }
        values.useSleepSession(durationMinutes, sleepStages(metric.getDetails()));
    }

    private void addSleepHours(
            Map<LocalDate, DailyMetrics> daily,
            HealthMetricEntity metric,
            ZoneId timezone
    ) {
        DailyMetrics values = daily.get(localDate(metric, timezone));
        BigDecimal metricValue = metric.getMetricValue();
        if (values != null && metricValue != null) {
            values.sleepPointMinutes = (values.sleepPointMinutes == null ? 0.0 : values.sleepPointMinutes)
                    + metricValue.doubleValue() * 60.0;
        }
    }

    private LocalDate localDate(HealthMetricEntity metric, ZoneId timezone) {
        return metric.getRecordedAt().atZoneSameInstant(timezone).toLocalDate();
    }

    private boolean isResting(Map<String, Object> details) {
        Object context = details == null ? null : details.get("measurementContext");
        return context != null && "RESTING".equalsIgnoreCase(context.toString());
    }

    private List<SleepStage> sleepStages(Map<String, Object> details) {
        Object rawStages = details == null ? null : details.get("stages");
        if (!(rawStages instanceof List<?> stages)) {
            return List.of();
        }
        List<SleepStage> result = new ArrayList<>();
        for (Object rawStage : stages) {
            if (!(rawStage instanceof Map<?, ?> stage)) {
                continue;
            }
            Object rawType = stage.get("stage");
            if (!(rawType instanceof Number type)) {
                continue;
            }
            try {
                OffsetDateTime start = OffsetDateTime.parse(String.valueOf(stage.get("startTime")));
                OffsetDateTime end = OffsetDateTime.parse(String.valueOf(stage.get("endTime")));
                long minutes = Duration.between(start, end).toMinutes();
                if (minutes > 0) {
                    result.add(new SleepStage(type.intValue(), minutes));
                }
            } catch (DateTimeParseException ignored) {
                // Invalid optional stage metadata is omitted; the session duration remains usable.
            }
        }
        return result;
    }

    private Double historyMean(
            Map<LocalDate, Map<String, Double>> daily,
            LocalDate featureDate,
            String feature,
            int days,
            int minimumValues
    ) {
        double sum = 0;
        int count = 0;
        for (int offset = 1; offset <= days; offset++) {
            Double value = daily.get(featureDate.minusDays(offset)).get(feature);
            if (value != null && Double.isFinite(value)) {
                sum += value;
                count++;
            }
        }
        return count < minimumValues ? null : sum / count;
    }

    private record SleepStage(int type, long minutes) {
    }

    private static final class DailyMetrics {
        private final List<Double> heartRates = new ArrayList<>();
        private final List<Double> restingHeartRates = new ArrayList<>();
        private Double stepsTotal;
        private double sleepSessionMinutes;
        private Double sleepPointMinutes;
        private Double sleepMinutes;
        private Double sleepEfficiency;
        private Double sleepDeepMinutes;
        private Double sleepRemMinutes;
        private Double sleepAwakeMinutes;

        private void useSleepSession(double sessionMinutes, List<SleepStage> stages) {
            sleepSessionMinutes = sessionMinutes;
            if (stages.isEmpty()) {
                sleepMinutes = sessionMinutes;
                sleepEfficiency = null;
                sleepDeepMinutes = null;
                sleepRemMinutes = null;
                sleepAwakeMinutes = null;
                return;
            }
            double asleep = stages.stream().filter(stage -> List.of(2, 4, 5, 6).contains(stage.type()))
                    .mapToLong(SleepStage::minutes).sum();
            sleepMinutes = asleep > 0 ? asleep : sessionMinutes;
            sleepEfficiency = Math.min(100.0, sleepMinutes / sessionMinutes * 100.0);
            sleepDeepMinutes = (double) stages.stream().filter(stage -> stage.type() == 5)
                    .mapToLong(SleepStage::minutes).sum();
            sleepRemMinutes = (double) stages.stream().filter(stage -> stage.type() == 6)
                    .mapToLong(SleepStage::minutes).sum();
            sleepAwakeMinutes = (double) stages.stream().filter(stage -> stage.type() == 1)
                    .mapToLong(SleepStage::minutes).sum();
        }

        private Map<String, Double> values() {
            Map<String, Double> values = new LinkedHashMap<>();
            values.put("hr_mean", average(heartRates));
            values.put("hr_std", populationStandardDeviation(heartRates));
            values.put("hr_count", heartRates.isEmpty() ? null : (double) heartRates.size());
            values.put("steps_total", stepsTotal);
            values.put("sleep_minutes", sleepMinutes != null ? sleepMinutes : sleepPointMinutes);
            values.put("sleep_efficiency", sleepEfficiency);
            values.put("sleep_deep_minutes", sleepDeepMinutes);
            values.put("sleep_rem_minutes", sleepRemMinutes);
            values.put("sleep_awake_minutes", sleepAwakeMinutes);
            values.put("resting_hr", average(restingHeartRates));
            return values;
        }

        private Double average(List<Double> values) {
            return values.isEmpty() ? null : values.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
        }

        private Double populationStandardDeviation(List<Double> values) {
            if (values.isEmpty()) {
                return null;
            }
            double mean = average(values);
            double variance = values.stream()
                    .mapToDouble(value -> (value - mean) * (value - mean))
                    .average()
                    .orElse(0.0);
            return Math.sqrt(variance);
        }
    }
}
