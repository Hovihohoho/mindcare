package com.mindcare.emotionservice.healthmetric.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mindcare.emotionservice.healthmetric.entity.HealthMetricEntity;
import com.mindcare.emotionservice.healthmetric.repository.HealthMetricRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WellnessFeatureServiceTest {
    private static final ZoneId TIMEZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate FEATURE_DATE = LocalDate.of(2026, 9, 9);

    private HealthMetricRepository repository;
    private WellnessFeatureService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        repository = mock(HealthMetricRepository.class);
        service = new WellnessFeatureService(
                repository,
                Clock.fixed(Instant.parse("2026-09-10T05:00:00Z"), ZoneOffset.UTC));
        userId = UUID.randomUUID();
        for (String type : List.of("HEART_RATE", "STEP_COUNT", "TOTAL_CALORIES_BURNED", "DISTANCE",
                "EXERCISE_SESSION", "SLEEP_SESSION", "SLEEP_HOURS")) {
            stub(type, List.of());
        }
    }

    @Test
    void buildsExactLifeSnapsOrderUnitsAndInclusiveCalendarTrends() {
        stub("HEART_RATE", List.of(
                point("HEART_RATE", FEATURE_DATE, 8, 60, Map.of("measurementContext", "RESTING")),
                point("HEART_RATE", FEATURE_DATE, 9, 80, Map.of())));
        List<HealthMetricEntity> steps = new ArrayList<>();
        steps.add(point("STEP_COUNT", FEATURE_DATE, 12, 4_000, Map.of()));
        for (int offset = 1; offset <= 6; offset++) {
            steps.add(point("STEP_COUNT", FEATURE_DATE.minusDays(offset), 12, offset * 1_000, Map.of()));
        }
        stub("STEP_COUNT", steps);
        stub("TOTAL_CALORIES_BURNED", List.of(point(
                "TOTAL_CALORIES_BURNED", FEATURE_DATE, 12, 2_100, Map.of())));
        stub("DISTANCE", List.of(point("DISTANCE", FEATURE_DATE, 12, 5_500, Map.of())));
        stub("EXERCISE_SESSION", List.of(session(
                "EXERCISE_SESSION",
                OffsetDateTime.parse("2026-09-09T17:00:00+07:00"),
                OffsetDateTime.parse("2026-09-09T17:45:00+07:00"),
                Map.of())));
        stub("SLEEP_SESSION", List.of(session(
                "SLEEP_SESSION",
                OffsetDateTime.parse("2026-09-08T23:00:00+07:00"),
                OffsetDateTime.parse("2026-09-09T07:00:00+07:00"),
                Map.of("stages", List.of(
                        stage(4, "2026-09-08T23:00:00+07:00", "2026-09-09T06:00:00+07:00"),
                        stage(1, "2026-09-09T06:00:00+07:00", "2026-09-09T07:00:00+07:00"))))));

        var response = service.build(userId, FEATURE_DATE, TIMEZONE);

        assertThat(response.featureVersion()).isEqualTo("lifesnaps-features-v1");
        assertThat(response.featureNames()).containsExactlyElementsOf(WellnessFeatureService.FEATURE_NAMES);
        assertThat(response.features()).hasSize(42);
        assertThat(response.features().subList(0, 18)).containsExactly(
                70.0, 10.0, 60.0, 80.0, 2.0, 60.0, 4_000.0, 2_100.0, 5_500.0,
                null, null, null, 45.0, null, 420.0, 480.0, 60.0, 87.5);
        assertThat(response.features().get(27)).isCloseTo(2_333.3333, within(0.0001));
        assertThat(response.features().get(28)).isCloseTo(3_571.4286, within(0.0001));
        assertThat(response.features().get(29)).isCloseTo(428.5714, within(0.0001));
        assertThat(response.availableBaseFeatureCount()).isEqualTo(14);
    }

    @Test
    void preservesMissingValuesAndRequiresLifeSnapsMinimumPeriods() {
        stub("STEP_COUNT", List.of(point("STEP_COUNT", FEATURE_DATE, 12, 2_500, Map.of())));

        var response = service.build(userId, FEATURE_DATE, TIMEZONE);

        assertThat(response.features().get(0)).isNull();
        assertThat(response.features().get(6)).isEqualTo(2_500.0);
        assertThat(response.features().get(27)).isNull();
        assertThat(response.features().get(28)).isNull();
        assertThat(response.availableBaseFeatureCount()).isOne();
    }

    @Test
    void convertsInvalidDailyTotalsToMissingInsteadOfFeedingOutliersToModel() {
        stub("STEP_COUNT", List.of(
                point("STEP_COUNT", FEATURE_DATE, 8, 60_000, Map.of()),
                point("STEP_COUNT", FEATURE_DATE, 18, 60_000, Map.of())));

        var response = service.build(userId, FEATURE_DATE, TIMEZONE);

        assertThat(response.features().get(6)).isNull();
        assertThat(response.availableBaseFeatureCount()).isZero();
    }

    private org.assertj.core.data.Offset<Double> within(double value) {
        return org.assertj.core.data.Offset.offset(value);
    }

    private void stub(String type, List<HealthMetricEntity> metrics) {
        when(repository
                .findByUserIdAndMetricTypeAndDeletedAtIsNullAndRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderByRecordedAtAsc(
                        eq(userId), eq(type), any(), any()))
                .thenReturn(metrics);
    }

    private HealthMetricEntity point(
            String type, LocalDate date, int hour, double value, Map<String, Object> details
    ) {
        HealthMetricEntity metric = mock(HealthMetricEntity.class);
        when(metric.getMetricType()).thenReturn(type);
        when(metric.getRecordedAt()).thenReturn(date.atTime(hour, 0).atZone(TIMEZONE).toOffsetDateTime());
        when(metric.getMetricValue()).thenReturn(BigDecimal.valueOf(value));
        when(metric.getDetails()).thenReturn(details);
        return metric;
    }

    private HealthMetricEntity session(
            String type, OffsetDateTime start, OffsetDateTime end, Map<String, Object> details
    ) {
        HealthMetricEntity metric = mock(HealthMetricEntity.class);
        when(metric.getMetricType()).thenReturn(type);
        when(metric.getRecordedAt()).thenReturn(end);
        when(metric.getStartTime()).thenReturn(start);
        when(metric.getEndTime()).thenReturn(end);
        when(metric.getDetails()).thenReturn(details);
        return metric;
    }

    private Map<String, Object> stage(int type, String start, String end) {
        return Map.of("stage", type, "startTime", start, "endTime", end);
    }
}
