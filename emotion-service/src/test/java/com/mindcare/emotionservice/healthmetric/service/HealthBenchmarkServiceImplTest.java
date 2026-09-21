package com.mindcare.emotionservice.healthmetric.service;

import com.mindcare.emotionservice.healthmetric.dto.HealthBenchmarkSnapshot;
import com.mindcare.emotionservice.healthmetric.dto.DailyHealthEvaluationResponse;
import com.mindcare.emotionservice.shared.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthBenchmarkServiceImplTest {

    private final HealthMetricService metrics = mock(HealthMetricService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-11T05:00:00Z"), ZoneOffset.UTC);
    private final HealthBenchmarkServiceImpl service = new HealthBenchmarkServiceImpl(metrics, clock);

    @Test
    void recommendsSleepAndActivityPlansOnlyAfterEnoughRepeatedData() {
        UUID userId = UUID.randomUUID();
        when(metrics.getBenchmarkSnapshot(userId, 7)).thenReturn(new HealthBenchmarkSnapshot(
                List.of(
                        day(1, "6.0", "4000"), day(2, "6.5", "4500"),
                        day(3, "6.8", "3000"), day(4, "7.2", "5000")
                ), List.of(), List.of()));

        var evaluations = service.evaluate(userId);

        assertThat(evaluations).filteredOn(item -> item.alertTriggered() && item.recommendedPlanTemplateCode() != null)
                .extracting(item -> item.recommendedPlanTemplateCode())
                .containsExactly("SLEEP_WELLNESS_V1", "LOW_ACTIVITY_MACTIVE_V1");
    }

    @Test
    void requiresRepeatedRestingHeartRateAndSpo2Measurements() {
        UUID userId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.parse("2026-08-28T08:00:00Z");
        when(metrics.getBenchmarkSnapshot(userId, 7)).thenReturn(new HealthBenchmarkSnapshot(
                List.of(),
                List.of(new HealthBenchmarkSnapshot.Observation(now.minusHours(1), BigDecimal.valueOf(110)),
                        new HealthBenchmarkSnapshot.Observation(now, BigDecimal.valueOf(105))),
                List.of(new HealthBenchmarkSnapshot.Observation(now.minusMinutes(10), BigDecimal.valueOf(94)),
                        new HealthBenchmarkSnapshot.Observation(now, BigDecimal.valueOf(93)))));

        var evaluations = service.evaluate(userId);

        assertThat(evaluations).filteredOn(item -> item.alertTriggered())
                .extracting(item -> item.reasonCode())
                .contains("RESTING_HEART_RATE_OUTSIDE_60_100_REPEATED", "SPO2_BELOW_95_REPEATED");
    }

    @Test
    void combinesGeneralReferencesWithPersonalBaselineForPreviousDay() {
        UUID userId = UUID.randomUUID();
        ZoneId timezone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate targetDate = LocalDate.of(2026, 9, 10);
        List<HealthBenchmarkSnapshot.DailySummary> days = new ArrayList<>();
        List<HealthBenchmarkSnapshot.Observation> heartRates = new ArrayList<>();
        List<HealthBenchmarkSnapshot.Observation> oxygen = new ArrayList<>();
        for (int offset = 7; offset >= 1; offset--) {
            LocalDate date = targetDate.minusDays(offset);
            days.add(new HealthBenchmarkSnapshot.DailySummary(date, new BigDecimal("8.0"), new BigDecimal("10000")));
            heartRates.add(observation(date, "70", timezone));
            oxygen.add(observation(date, "98", timezone));
        }
        days.add(new HealthBenchmarkSnapshot.DailySummary(targetDate, new BigDecimal("5.5"), new BigDecimal("5000")));
        heartRates.add(observation(targetDate, "104", timezone));
        heartRates.add(observation(targetDate, "106", timezone));
        oxygen.add(observation(targetDate, "93", timezone));
        oxygen.add(observation(targetDate, "94", timezone));
        when(metrics.getBenchmarkSnapshot(userId, targetDate, 29, timezone))
                .thenReturn(new HealthBenchmarkSnapshot(days, heartRates, oxygen));

        var response = service.evaluateDay(userId, null, timezone);

        assertThat(response.evaluationDate()).isEqualTo(targetDate);
        assertThat(response.clinicalDiagnosis()).isFalse();
        assertThat(response.signals()).extracting(DailyHealthEvaluationResponse.Signal::status)
                .containsExactly(
                        "BELOW_SCIENTIFIC_BENCHMARK",
                        "WITHIN_SCIENTIFIC_BENCHMARK",
                        "RECHECK_RECOMMENDED",
                        "RECHECK_RECOMMENDED"
                );
        assertThat(response.signals()).allSatisfy(signal -> {
            assertThat(signal.personalBaselineDays()).isEqualTo(7);
            assertThat(signal.sourceUrl()).startsWith("https://");
        });
        assertThat(response.signals().get(0).generalThresholdTriggered()).isTrue();
        assertThat(response.signals().get(0).personalDeviationTriggered()).isTrue();
        assertThat(response.signals().get(1).generalThresholdTriggered()).isFalse();
        assertThat(response.signals().get(1).personalDeviationTriggered()).isTrue();
    }

    @Test
    void doesNotTreatMissingBaselineAsNormalOrEvaluateAnIncompleteDay() {
        UUID userId = UUID.randomUUID();
        ZoneId timezone = ZoneId.of("Asia/Ho_Chi_Minh");
        LocalDate targetDate = LocalDate.of(2026, 9, 10);
        when(metrics.getBenchmarkSnapshot(userId, targetDate, 29, timezone))
                .thenReturn(new HealthBenchmarkSnapshot(
                        List.of(new HealthBenchmarkSnapshot.DailySummary(targetDate, new BigDecimal("7.5"), new BigDecimal("3000"))),
                        List.of(),
                        List.of()));

        var response = service.evaluateDay(userId, targetDate, timezone);

        assertThat(response.signals()).extracting(DailyHealthEvaluationResponse.Signal::status)
                .containsExactly(
                        "WITHIN_SCIENTIFIC_BENCHMARK",
                        "BELOW_SCIENTIFIC_BENCHMARK",
                        "NO_DATA",
                        "NO_DATA"
                );
        assertThatThrownBy(() -> service.evaluateDay(userId, LocalDate.of(2026, 9, 11), timezone))
                .isInstanceOf(InvalidRequestException.class);
    }

    private HealthBenchmarkSnapshot.DailySummary day(int day, String sleep, String steps) {
        return new HealthBenchmarkSnapshot.DailySummary(LocalDate.of(2026, 8, day),
                new BigDecimal(sleep), new BigDecimal(steps));
    }

    private HealthBenchmarkSnapshot.Observation observation(LocalDate date, String value, ZoneId timezone) {
        return new HealthBenchmarkSnapshot.Observation(
                date.atTime(8, 0).atZone(timezone).toOffsetDateTime(), new BigDecimal(value));
    }
}
