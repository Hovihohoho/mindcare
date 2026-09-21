package com.mindcare.emotionservice.healthmetric.service;

import com.mindcare.emotionservice.healthmetric.dto.WellnessFeatureResponse;
import com.mindcare.emotionservice.healthmetric.entity.HealthMetricEntity;
import com.mindcare.emotionservice.healthmetric.repository.HealthMetricRepository;
import com.mindcare.emotionservice.shared.exception.InvalidRequestException;
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
public class WellnessFeatureService {
    public static final String FEATURE_VERSION = "lifesnaps-features-v1";
    public static final List<String> FEATURE_NAMES = List.of(
            "hr_mean", "hr_std", "hr_min", "hr_max", "hr_hour_count",
            "resting_hr", "steps_total", "calories_total", "distance_total",
            "lightly_active_minutes", "moderately_active_minutes", "very_active_minutes",
            "active_minutes", "sedentary_minutes", "sleep_minutes",
            "sleep_duration_minutes", "sleep_awake_minutes", "sleep_efficiency",
            "hr_mean_mean_3d", "hr_mean_mean_7d", "hr_mean_delta_7d",
            "hr_std_mean_3d", "hr_std_mean_7d", "hr_std_delta_7d",
            "resting_hr_mean_3d", "resting_hr_mean_7d", "resting_hr_delta_7d",
            "steps_total_mean_3d", "steps_total_mean_7d", "steps_total_delta_7d",
            "active_minutes_mean_3d", "active_minutes_mean_7d", "active_minutes_delta_7d",
            "sedentary_minutes_mean_3d", "sedentary_minutes_mean_7d", "sedentary_minutes_delta_7d",
            "sleep_minutes_mean_3d", "sleep_minutes_mean_7d", "sleep_minutes_delta_7d",
            "sleep_efficiency_mean_3d", "sleep_efficiency_mean_7d", "sleep_efficiency_delta_7d");
    private static final List<String> BASE_FEATURES = FEATURE_NAMES.subList(0, 18);
    private static final List<String> TREND_SOURCES = List.of(
            "hr_mean", "hr_std", "resting_hr", "steps_total", "active_minutes",
            "sedentary_minutes", "sleep_minutes", "sleep_efficiency");
    private static final List<String> METRIC_TYPES = List.of(
            "HEART_RATE", "STEP_COUNT", "TOTAL_CALORIES_BURNED", "DISTANCE",
            "EXERCISE_SESSION", "SLEEP_SESSION", "SLEEP_HOURS");

    private final HealthMetricRepository repository;
    private final Clock clock;

    public WellnessFeatureService(HealthMetricRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public WellnessFeatureResponse build(UUID userId, LocalDate requestedDate, ZoneId timezone) {
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

        // pandas rolling("7D") includes the current date and the previous six calendar days.
        LocalDate firstDate = featureDate.minusDays(6);
        OffsetDateTime from = firstDate.atStartOfDay(timezone).toOffsetDateTime();
        OffsetDateTime to = featureDate.plusDays(1).atStartOfDay(timezone).toOffsetDateTime();
        Map<LocalDate, DailyMetrics> daily = new LinkedHashMap<>();
        for (int offset = 6; offset >= 0; offset--) {
            daily.put(featureDate.minusDays(offset), new DailyMetrics());
        }
        for (String type : METRIC_TYPES) {
            load(userId, type, from, to).forEach(metric -> add(daily, metric, timezone));
        }

        Map<LocalDate, Map<String, Double>> valuesByDate = new LinkedHashMap<>();
        daily.forEach((date, metrics) -> valuesByDate.put(date, metrics.values()));
        Map<String, Double> current = valuesByDate.get(featureDate);
        List<Double> features = new ArrayList<>(FEATURE_NAMES.size());
        BASE_FEATURES.forEach(name -> features.add(current.get(name)));
        for (String source : TREND_SOURCES) {
            Double mean3 = rollingMean(valuesByDate, featureDate, source, 3, 2);
            Double mean7 = rollingMean(valuesByDate, featureDate, source, 7, 3);
            features.add(mean3);
            features.add(mean7);
            features.add(current.get(source) == null || mean7 == null ? null : current.get(source) - mean7);
        }
        int available = (int) features.subList(0, BASE_FEATURES.size()).stream()
                .filter(value -> value != null && Double.isFinite(value))
                .count();
        return new WellnessFeatureResponse(
                FEATURE_VERSION,
                featureDate,
                timezone.getId(),
                FEATURE_NAMES,
                Collections.unmodifiableList(features),
                available);
    }

    private List<HealthMetricEntity> load(UUID userId, String type, OffsetDateTime from, OffsetDateTime to) {
        return repository
                .findByUserIdAndMetricTypeAndDeletedAtIsNullAndRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderByRecordedAtAsc(
                        userId, type, from, to);
    }

    private void add(Map<LocalDate, DailyMetrics> daily, HealthMetricEntity metric, ZoneId timezone) {
        DailyMetrics values = daily.get(localDate(metric, timezone));
        if (values == null) {
            return;
        }
        switch (metric.getMetricType()) {
            case "HEART_RATE" -> values.addHeartRate(metric, timezone);
            case "STEP_COUNT" -> values.stepsTotal = add(values.stepsTotal, bounded(metric, 0, 100_000));
            case "TOTAL_CALORIES_BURNED" -> values.caloriesTotal = add(values.caloriesTotal, bounded(metric, 0, 15_000));
            case "DISTANCE" -> values.distanceTotal = add(values.distanceTotal, bounded(metric, 0, 100_000));
            case "EXERCISE_SESSION" -> values.addExercise(metric);
            case "SLEEP_SESSION" -> values.addSleepSession(metric);
            case "SLEEP_HOURS" -> values.addSleepHours(metric);
            default -> {
                // The fixed list above makes this unreachable and keeps unknown metrics out of the contract.
            }
        }
    }

    private Double bounded(HealthMetricEntity metric, double minimum, double maximum) {
        if (metric.getMetricValue() == null) {
            return null;
        }
        double value = metric.getMetricValue().doubleValue();
        return Double.isFinite(value) && value >= minimum && value <= maximum ? value : null;
    }

    private Double add(Double current, Double value) {
        return value == null ? current : (current == null ? value : current + value);
    }

    private LocalDate localDate(HealthMetricEntity metric, ZoneId timezone) {
        return metric.getRecordedAt().atZoneSameInstant(timezone).toLocalDate();
    }

    private Double rollingMean(
            Map<LocalDate, Map<String, Double>> daily,
            LocalDate featureDate,
            String feature,
            int days,
            int minimumValues
    ) {
        double sum = 0;
        int count = 0;
        for (int offset = 0; offset < days; offset++) {
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
        private final Map<Integer, List<Double>> heartRatesByHour = new LinkedHashMap<>();
        private final List<Double> restingHeartRates = new ArrayList<>();
        private Double stepsTotal;
        private Double caloriesTotal;
        private Double distanceTotal;
        private Double activeMinutes;
        private double longestSleepSession;
        private Double sleepPointMinutes;
        private Double sleepMinutes;
        private Double sleepDurationMinutes;
        private Double sleepAwakeMinutes;
        private Double sleepEfficiency;

        private void addHeartRate(HealthMetricEntity metric, ZoneId timezone) {
            if (metric.getMetricValue() == null) {
                return;
            }
            double value = metric.getMetricValue().doubleValue();
            if (!Double.isFinite(value) || value < 20 || value > 250) {
                return;
            }
            int localHour = metric.getRecordedAt().atZoneSameInstant(timezone).getHour();
            heartRatesByHour.computeIfAbsent(localHour, ignored -> new ArrayList<>()).add(value);
            Object context = metric.getDetails() == null ? null : metric.getDetails().get("measurementContext");
            if (context != null && "RESTING".equalsIgnoreCase(context.toString()) && value <= 200) {
                restingHeartRates.add(value);
            }
        }

        private void addExercise(HealthMetricEntity metric) {
            Double duration = durationMinutes(metric);
            if (duration != null && duration <= 1_440) {
                activeMinutes = activeMinutes == null ? duration : activeMinutes + duration;
                if (activeMinutes > 1_440) {
                    activeMinutes = null;
                }
            }
        }

        private void addSleepSession(HealthMetricEntity metric) {
            Double duration = durationMinutes(metric);
            if (duration == null || duration > 1_440 || duration <= longestSleepSession) {
                return;
            }
            longestSleepSession = duration;
            sleepDurationMinutes = duration;
            List<SleepStage> stages = sleepStages(metric.getDetails());
            if (stages.isEmpty()) {
                sleepMinutes = duration;
                sleepAwakeMinutes = null;
                sleepEfficiency = null;
                return;
            }
            double asleep = stages.stream().filter(stage -> List.of(2, 4, 5, 6).contains(stage.type()))
                    .mapToLong(SleepStage::minutes).sum();
            double awake = stages.stream().filter(stage -> stage.type() == 1)
                    .mapToLong(SleepStage::minutes).sum();
            sleepMinutes = asleep > 0 ? asleep : duration;
            sleepAwakeMinutes = awake;
            sleepEfficiency = Math.min(100.0, sleepMinutes / duration * 100.0);
        }

        private void addSleepHours(HealthMetricEntity metric) {
            if (metric.getMetricValue() == null) {
                return;
            }
            double minutes = metric.getMetricValue().doubleValue() * 60.0;
            if (Double.isFinite(minutes) && minutes > 0 && minutes <= 1_440) {
                sleepPointMinutes = sleepPointMinutes == null ? minutes : sleepPointMinutes + minutes;
            }
        }

        private Map<String, Double> values() {
            List<Double> raw = heartRatesByHour.values().stream().flatMap(List::stream).toList();
            List<Double> hourly = heartRatesByHour.values().stream().map(DailyMetrics::average).toList();
            Double effectiveSleep = boundedTotal(sleepMinutes != null ? sleepMinutes : sleepPointMinutes, 1_440);
            Map<String, Double> values = new LinkedHashMap<>();
            values.put("hr_mean", average(raw));
            values.put("hr_std", standardDeviation(hourly));
            values.put("hr_min", minimum(hourly));
            values.put("hr_max", maximum(hourly));
            values.put("hr_hour_count", hourly.isEmpty() ? null : (double) hourly.size());
            values.put("resting_hr", average(restingHeartRates));
            values.put("steps_total", boundedTotal(stepsTotal, 100_000));
            values.put("calories_total", boundedTotal(caloriesTotal, 15_000));
            values.put("distance_total", boundedTotal(distanceTotal, 100_000));
            values.put("lightly_active_minutes", null);
            values.put("moderately_active_minutes", null);
            values.put("very_active_minutes", null);
            values.put("active_minutes", boundedTotal(activeMinutes, 1_440));
            values.put("sedentary_minutes", null);
            values.put("sleep_minutes", effectiveSleep);
            values.put("sleep_duration_minutes", boundedTotal(
                    sleepDurationMinutes != null ? sleepDurationMinutes : effectiveSleep, 1_440));
            values.put("sleep_awake_minutes", sleepAwakeMinutes);
            values.put("sleep_efficiency", sleepEfficiency);
            return values;
        }

        private static Double durationMinutes(HealthMetricEntity metric) {
            if (metric.getStartTime() == null || metric.getEndTime() == null) {
                return null;
            }
            long minutes = Duration.between(metric.getStartTime(), metric.getEndTime()).toMinutes();
            return minutes > 0 ? (double) minutes : null;
        }

        private static List<SleepStage> sleepStages(Map<String, Object> details) {
            Object rawStages = details == null ? null : details.get("stages");
            if (!(rawStages instanceof List<?> stages)) {
                return List.of();
            }
            List<SleepStage> result = new ArrayList<>();
            for (Object rawStage : stages) {
                if (!(rawStage instanceof Map<?, ?> stage) || !(stage.get("stage") instanceof Number type)) {
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
                    // Invalid optional stage metadata is omitted; session duration remains usable.
                }
            }
            return result;
        }

        private static Double average(List<Double> values) {
            return values.isEmpty() ? null : values.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
        }

        private static Double standardDeviation(List<Double> values) {
            if (values.isEmpty()) {
                return null;
            }
            double mean = average(values);
            double variance = values.stream().mapToDouble(value -> (value - mean) * (value - mean))
                    .average().orElse(0.0);
            return Math.sqrt(variance);
        }

        private static Double minimum(List<Double> values) {
            return values.stream().mapToDouble(Double::doubleValue).min().stream().boxed().findFirst().orElse(null);
        }

        private static Double maximum(List<Double> values) {
            return values.stream().mapToDouble(Double::doubleValue).max().stream().boxed().findFirst().orElse(null);
        }

        private static Double boundedTotal(Double value, double maximum) {
            return value != null && Double.isFinite(value) && value >= 0 && value <= maximum ? value : null;
        }
    }
}
