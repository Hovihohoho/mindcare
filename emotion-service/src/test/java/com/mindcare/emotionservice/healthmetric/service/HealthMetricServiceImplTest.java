package com.mindcare.emotionservice.healthmetric.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindcare.emotionservice.healthmetric.dto.HealthMetricBatchRequest;
import com.mindcare.emotionservice.healthmetric.dto.HealthMetricItemRequest;
import com.mindcare.emotionservice.healthmetric.entity.HealthMetricEntity;
import com.mindcare.emotionservice.healthmetric.mapper.HealthMetricMapper;
import com.mindcare.emotionservice.healthmetric.repository.HealthMetricRepository;
import com.mindcare.emotionservice.healthmetric.repository.HealthMetricSyncRequestRepository;
import com.mindcare.emotionservice.shared.exception.InvalidRequestException;
import com.mindcare.emotionservice.shared.util.CursorCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthMetricServiceImplTest {

    @Mock
    private HealthMetricRepository repository;
    @Mock
    private HealthMetricSyncRequestRepository syncRequestRepository;
    @Mock
    private HealthMetricMapper mapper;

    private HealthMetricServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new HealthMetricServiceImpl(
                repository,
                syncRequestRepository,
                mapper,
                new CursorCodec(),
                new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-07-22T00:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void synchronizeConvertsSleepMinutesToCanonicalHours() {
        UUID userId = UUID.randomUUID();
        HealthMetricEntity savedEntity = mock(HealthMetricEntity.class);
        when(savedEntity.getId()).thenReturn(UUID.randomUUID());
        when(syncRequestRepository.findByUserIdAndSourceTypeAndIdempotencyKey(eq(userId), anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(repository.existsByUserIdAndSourceTypeAndExternalSampleIdAndDeletedAtIsNull(
                eq(userId), anyString(), anyString()
        )).thenReturn(false);
        when(repository.saveAllAndFlush(any())).thenReturn(List.of(savedEntity));

        service.synchronizeMetrics(
                userId,
                "sync-1",
                new HealthMetricBatchRequest("APPLE_HEALTH", List.of(new HealthMetricItemRequest(
                        "sample-1",
                        "SLEEP_HOURS",
                        BigDecimal.valueOf(120),
                        "min",
                        OffsetDateTime.parse("2026-07-21T22:00:00Z")
                )))
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<HealthMetricEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAllAndFlush(captor.capture());
        assertEquals(new BigDecimal("2.00"), captor.getValue().get(0).getMetricValue());
        assertEquals("h", captor.getValue().get(0).getUnit());
    }

    @Test
    void synchronizeRejectsDeviceSampleWithoutExternalId() {
        when(syncRequestRepository.findByUserIdAndSourceTypeAndIdempotencyKey(any(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        assertThrows(InvalidRequestException.class, () -> service.synchronizeMetrics(
                UUID.randomUUID(),
                "sync-2",
                new HealthMetricBatchRequest("GOOGLE_HEALTH", List.of(new HealthMetricItemRequest(
                        null,
                        "HEART_RATE",
                        BigDecimal.valueOf(70),
                        "bpm",
                        OffsetDateTime.parse("2026-07-21T22:00:00Z")
                )))
        ));
    }
}
