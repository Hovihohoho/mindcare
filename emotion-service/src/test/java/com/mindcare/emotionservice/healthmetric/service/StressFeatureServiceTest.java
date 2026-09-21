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

class StressFeatureServiceTest {
    private static final ZoneId TIMEZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDate FEATURE_DATE = LocalDate.of(2026, 9, 9);

    private HealthMetricRepository repository;
    private StressFeatureService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        repository = mock(HealthMetricRepository.class);
        service = new StressFeatureService(
                repository,
                Clock.fixed(Instant.parse("2026-09-10T05:00:00Z"), ZoneOffset.UTC));
        userId = UUID.randomUUID();
    }

    @Test
    void buildsPmdataFeatureOrderAndTrailingTrends() {
        List<HealthMetricEntity> heartRates = List.of(
                point(FEATURE_DATE, new BigDecimal("60"), Map.of("measurementContext", "RESTING")),
                point(FEATURE_DATE, new BigDecimal("80"), Map.of()));
        List<HealthMetricEntity> steps = new ArrayList<>();
        steps.add(point(FEATURE_DATE, new BigDecimal("4000"), Map.of()));
        for (int offset = 1; offset <= 7; offset++) {
            steps.add(point(FEATURE_DATE.minusDays(offset), BigDecimal.valueOf(offset * 1000L), Map.of()));
        }
        HealthMetricEntity sleep = session(
                OffsetDateTime.parse("2026-09-08T23:00:00+07:00"),
                OffsetDateTime.parse("2026-09-09T07:00:00+07:00"),
                List.of(
                        stage(4, "2026-09-08T23:00:00+07:00", "2026-09-09T04:00:00+07:00"),
                        stage(5, "2026-09-09T04:00:00+07:00", "2026-09-09T05:00:00+07:00"),
                        stage(6, "2026-09-09T05:00:00+07:00", "2026-09-09T06:00:00+07:00"),
                        stage(1, "2026-09-09T06:00:00+07:00", "2026-09-09T07:00:00+07:00")));
        stub("HEART_RATE", heartRates);
        stub("STEP_COUNT", steps);
        stub("SLEEP_SESSION", List.of(sleep));
        stub("SLEEP_HOURS", List.of());

        var response = service.build(userId, FEATURE_DATE, TIMEZONE);

        assertThat(response.featureNames()).containsExactlyElementsOf(StressFeatureService.FEATURE_NAMES);
        assertThat(response.features()).hasSize(25);
        assertThat(response.features().subList(0, 10)).containsExactly(
                70.0, 10.0, 2.0, 4000.0, 420.0, 87.5, 60.0, 60.0, 60.0, 60.0);
        assertThat(response.features().get(13)).isEqualTo(2000.0);
        assertThat(response.features().get(14)).isEqualTo(4000.0);
        assertThat(response.features().get(15)).isEqualTo(0.0);
        assertThat(response.availableBaseFeatureCount()).isEqualTo(10);
        assertThat(response.featureVersion()).isEqualTo("pmdata-features-v2");
    }

    @Test
    void preservesMissingHealthDataAsNull() {
        stub("HEART_RATE", List.of());
        stub("STEP_COUNT", List.of(point(FEATURE_DATE, new BigDecimal("2500"), Map.of())));
        stub("SLEEP_SESSION", List.of());
        stub("SLEEP_HOURS", List.of());

        var response = service.build(userId, FEATURE_DATE, TIMEZONE);

        assertThat(response.features().get(0)).isNull();
        assertThat(response.features().get(3)).isEqualTo(2500.0);
        assertThat(response.features().get(13)).isNull();
        assertThat(response.availableBaseFeatureCount()).isOne();
    }

    private void stub(String type, List<HealthMetricEntity> metrics) {
        when(repository
                .findByUserIdAndMetricTypeAndDeletedAtIsNullAndRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderByRecordedAtAsc(
                        eq(userId), eq(type), any(), any()))
                .thenReturn(metrics);
    }

    private HealthMetricEntity point(LocalDate date, BigDecimal value, Map<String, Object> details) {
        HealthMetricEntity metric = mock(HealthMetricEntity.class);
        when(metric.getRecordedAt()).thenReturn(date.atTime(12, 0).atZone(TIMEZONE).toOffsetDateTime());
        when(metric.getMetricValue()).thenReturn(value);
        when(metric.getDetails()).thenReturn(details);
        return metric;
    }

    private HealthMetricEntity session(
            OffsetDateTime start,
            OffsetDateTime end,
            List<Map<String, Object>> stages
    ) {
        HealthMetricEntity metric = mock(HealthMetricEntity.class);
        when(metric.getRecordedAt()).thenReturn(end);
        when(metric.getStartTime()).thenReturn(start);
        when(metric.getEndTime()).thenReturn(end);
        when(metric.getDetails()).thenReturn(Map.of("stages", stages));
        return metric;
    }

    private Map<String, Object> stage(int type, String start, String end) {
        return Map.of("stage", type, "startTime", start, "endTime", end);
    }
}
