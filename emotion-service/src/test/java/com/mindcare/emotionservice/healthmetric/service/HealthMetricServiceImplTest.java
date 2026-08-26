package com.mindcare.emotionservice.healthmetric.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindcare.emotionservice.healthmetric.dto.HealthMetricBatchRequest;
import com.mindcare.emotionservice.healthmetric.dto.HealthMetricItemRequest;
import com.mindcare.emotionservice.healthmetric.entity.HealthMetricEntity;
import com.mindcare.emotionservice.healthmetric.entity.HealthSourceConsentEntity;
import com.mindcare.emotionservice.healthmetric.mapper.HealthMetricMapper;
import com.mindcare.emotionservice.healthmetric.repository.HealthMetricRepository;
import com.mindcare.emotionservice.healthmetric.repository.HealthMetricSyncRequestRepository;
import com.mindcare.emotionservice.healthmetric.repository.HealthSourceConsentRepository;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    private HealthSourceConsentRepository consentRepository;
    @Mock
    private HealthMetricMapper mapper;

    private HealthMetricServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new HealthMetricServiceImpl(
                repository,
                syncRequestRepository,
                consentRepository,
                mapper,
                new CursorCodec(),
                new ObjectMapper().findAndRegisterModules(),
                Clock.fixed(Instant.parse("2026-07-22T00:00:00Z"), ZoneOffset.UTC)
        );
        org.mockito.Mockito.lenient().when(consentRepository.findByUserIdAndSourceType(any(), anyString()))
                .thenReturn(Optional.empty());
        org.mockito.Mockito.lenient().when(consentRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void synchronizeConvertsSleepMinutesToCanonicalHours() {
        UUID userId = UUID.randomUUID();
        HealthMetricEntity savedEntity = mock(HealthMetricEntity.class);
        when(savedEntity.getId()).thenReturn(UUID.randomUUID());
        when(syncRequestRepository.findByUserIdAndSourceTypeAndIdempotencyKey(eq(userId), anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(repository.findByUserIdAndSourceTypeAndExternalSampleId(eq(userId), anyString(), anyString()))
                .thenReturn(Optional.empty());
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
    void synchronizeRejectsSourceAfterConsentWasRevoked() {
        UUID userId = UUID.randomUUID();
        HealthSourceConsentEntity consent = new HealthSourceConsentEntity(userId, "HEALTH_CONNECT");
        consent.revoke(OffsetDateTime.parse("2026-07-21T00:00:00Z"));
        when(consentRepository.findByUserIdAndSourceType(userId, "HEALTH_CONNECT"))
                .thenReturn(Optional.of(consent));

        assertThrows(InvalidRequestException.class, () -> service.synchronizeMetrics(
                userId, "sync-revoked", new HealthMetricBatchRequest("HEALTH_CONNECT", List.of(
                        new HealthMetricItemRequest("steps-1", "STEP_COUNT", BigDecimal.TEN, "count",
                                OffsetDateTime.parse("2026-07-21T22:00:00Z"))))));
    }

    @Test
    void revokeDeletesSourceRecordsAndDisablesFutureSync() {
        UUID userId = UUID.randomUUID();
        HealthSourceConsentEntity consent = new HealthSourceConsentEntity(userId, "HEALTH_CONNECT");
        when(consentRepository.findByUserIdAndSourceType(userId, "HEALTH_CONNECT"))
                .thenReturn(Optional.of(consent));
        when(consentRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.findActiveRange(userId, "HEALTH_CONNECT")).thenReturn(new Object[] { null, null });
        when(repository.countActiveByMetricType(userId, "HEALTH_CONNECT")).thenReturn(List.of());

        var response = service.revokeAndDeleteSourceData(userId, "HEALTH_CONNECT");

        assertFalse(response.syncEnabled());
        verify(syncRequestRepository).deleteByUserIdAndSourceType(userId, "HEALTH_CONNECT");
        verify(repository).deleteByUserIdAndSourceType(userId, "HEALTH_CONNECT");
    }

    @Test
    void synchronizeRejectsDeviceSampleWithoutExternalId() {
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

    @Test
    void synchronizeUpdatesRecordWhenSourceHasNewerRevision() {
        UUID userId = UUID.randomUUID();
        OffsetDateTime originalModifiedAt = OffsetDateTime.parse("2026-07-21T20:00:00Z");
        OffsetDateTime newModifiedAt = OffsetDateTime.parse("2026-07-21T21:00:00Z");
        HealthMetricEntity existing = new HealthMetricEntity(
                userId, "STEP_COUNT", BigDecimal.TEN, "count", "HEALTH_CONNECT", "steps-1",
                OffsetDateTime.parse("2026-07-21T20:00:00Z"), null, null, null, "watch.app",
                originalModifiedAt, Map.of()
        );
        when(syncRequestRepository.findByUserIdAndSourceTypeAndIdempotencyKey(eq(userId), anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(repository.findByUserIdAndSourceTypeAndExternalSampleId(userId, "HEALTH_CONNECT", "steps-1"))
                .thenReturn(Optional.of(existing));
        HealthMetricEntity savedEntity = mock(HealthMetricEntity.class);
        when(savedEntity.getId()).thenReturn(UUID.randomUUID());
        when(repository.saveAllAndFlush(any())).thenReturn(List.of(savedEntity));

        var response = service.synchronizeMetrics(userId, "sync-update", new HealthMetricBatchRequest(
                "HEALTH_CONNECT",
                List.of(new HealthMetricItemRequest(
                        "steps-1", "STEP_COUNT", BigDecimal.valueOf(25), "count",
                        OffsetDateTime.parse("2026-07-21T20:30:00Z"), null, null,
                        "Watch", "watch.app", newModifiedAt, Map.of()
                ))
        ));

        assertEquals(0, response.acceptedCount());
        assertEquals(1, response.updatedCount());
        assertEquals(BigDecimal.valueOf(25), existing.getMetricValue());
        assertEquals(newModifiedAt, existing.getSourceLastModifiedAt());
    }
}
