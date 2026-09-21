package com.mindcare.emotionservice.healthmetric.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.mindcare.emotionservice.healthmetric.entity.HealthMetricEntity;
import com.mindcare.emotionservice.healthmetric.repository.HealthMetricRepository;
import com.mindcare.emotionservice.support.AbstractPostgreSqlIntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class StressFeatureServicePostgreSqlIntegrationTest extends AbstractPostgreSqlIntegrationTest {
    private static final ZoneId TIMEZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Autowired
    private StressFeatureService service;

    @Autowired
    private HealthMetricRepository repository;

    @Test
    void readsOwnedActiveMetricsAndBuildsModelInput() {
        UUID userId = UUID.randomUUID();
        LocalDate featureDate = LocalDate.now(TIMEZONE).minusDays(1);
        List<HealthMetricEntity> metrics = new ArrayList<>();
        metrics.add(point(userId, "HEART_RATE", new BigDecimal("65"), featureDate,
                Map.of("measurementContext", "RESTING"), "heart-resting"));
        metrics.add(point(userId, "HEART_RATE", new BigDecimal("75"), featureDate,
                Map.of(), "heart-active"));
        for (int offset = 0; offset <= 7; offset++) {
            metrics.add(point(userId, "STEP_COUNT", BigDecimal.valueOf(1000L + offset * 500L),
                    featureDate.minusDays(offset), Map.of(), "steps-" + offset));
        }
        repository.saveAllAndFlush(metrics);

        var response = service.build(userId, featureDate, TIMEZONE);

        assertThat(response.features()).hasSize(25);
        assertThat(response.features().get(0)).isEqualTo(70.0);
        assertThat(response.features().get(1)).isEqualTo(5.0);
        assertThat(response.features().get(2)).isEqualTo(2.0);
        assertThat(response.features().get(3)).isEqualTo(1000.0);
        assertThat(response.features().get(9)).isEqualTo(65.0);
        assertThat(response.features().get(14)).isEqualTo(3000.0);
    }

    private HealthMetricEntity point(
            UUID userId,
            String type,
            BigDecimal value,
            LocalDate date,
            Map<String, Object> details,
            String externalId
    ) {
        OffsetDateTime recordedAt = date.atTime(12, 0).atZone(TIMEZONE).toOffsetDateTime();
        return new HealthMetricEntity(
                userId,
                type,
                value,
                "HEART_RATE".equals(type) ? "bpm" : "count",
                "HEALTH_CONNECT",
                externalId,
                recordedAt,
                null,
                null,
                "test-device",
                "test.origin",
                recordedAt,
                details);
    }
}
