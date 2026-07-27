package com.mindcare.emotionservice.healthmetric.service;

import com.mindcare.emotionservice.healthmetric.dto.HealthMetricBatchRequest;
import com.mindcare.emotionservice.healthmetric.dto.HealthMetricBatchResponse;
import com.mindcare.emotionservice.healthmetric.dto.HealthMetricItemRequest;
import com.mindcare.emotionservice.healthmetric.entity.HealthMetricEntity;
import com.mindcare.emotionservice.healthmetric.repository.HealthMetricRepository;
import com.mindcare.emotionservice.healthmetric.repository.HealthMetricSyncRequestRepository;
import com.mindcare.emotionservice.shared.exception.InvalidRequestException;
import com.mindcare.emotionservice.shared.exception.ResourceConflictException;
import com.mindcare.emotionservice.support.AbstractPostgreSqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HealthMetricServicePostgreSqlIntegrationTest extends AbstractPostgreSqlIntegrationTest {

    @Autowired
    private HealthMetricService healthMetricService;

    @Autowired
    private HealthMetricRepository metricRepository;

    @Autowired
    private HealthMetricSyncRequestRepository syncRequestRepository;

    @Test
    void synchronizationIsAtomicCanonicalAndIdempotent() {
        UUID userId = UUID.randomUUID();
        OffsetDateTime recordedAt = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1);
        HealthMetricBatchRequest request = new HealthMetricBatchRequest(
                "APPLE_HEALTH",
                List.of(new HealthMetricItemRequest(
                        "apple-sleep-001",
                        "SLEEP_HOURS",
                        BigDecimal.valueOf(120),
                        "min",
                        recordedAt
                ))
        );

        HealthMetricBatchResponse first = healthMetricService.synchronizeMetrics(userId, "sync-key-001", request);
        HealthMetricBatchResponse retry = healthMetricService.synchronizeMetrics(userId, "sync-key-001", request);

        assertThat(first.acceptedCount()).isOne();
        assertThat(retry).isEqualTo(first);
        assertThat(metricRepository.count()).isOne();
        assertThat(syncRequestRepository.count()).isOne();
        HealthMetricEntity stored = metricRepository.findAll().get(0);
        assertThat(stored.getMetricValue()).isEqualByComparingTo("2.00");
        assertThat(stored.getUnit()).isEqualTo("h");
        assertThat(stored.getExternalSampleId()).isEqualTo("apple-sleep-001");

        HealthMetricBatchRequest changedPayload = new HealthMetricBatchRequest(
                "APPLE_HEALTH",
                List.of(new HealthMetricItemRequest(
                        "apple-sleep-001",
                        "SLEEP_HOURS",
                        BigDecimal.valueOf(180),
                        "min",
                        recordedAt
                ))
        );
        assertThatThrownBy(() -> healthMetricService.synchronizeMetrics(
                userId,
                "sync-key-001",
                changedPayload
        )).isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("different payload");

        HealthMetricBatchRequest invalidAtomicBatch = new HealthMetricBatchRequest(
                "APPLE_HEALTH",
                List.of(
                        new HealthMetricItemRequest(
                                "apple-heart-001",
                                "HEART_RATE",
                                BigDecimal.valueOf(70),
                                "bpm",
                                recordedAt
                        ),
                        new HealthMetricItemRequest(
                                null,
                                "HEART_RATE",
                                BigDecimal.valueOf(75),
                                "bpm",
                                recordedAt
                        )
                )
        );
        assertThatThrownBy(() -> healthMetricService.synchronizeMetrics(
                userId,
                "sync-key-002",
                invalidAtomicBatch
        )).isInstanceOf(InvalidRequestException.class);
        assertThat(metricRepository.count()).isOne();
        assertThat(syncRequestRepository.count()).isOne();
    }
}
