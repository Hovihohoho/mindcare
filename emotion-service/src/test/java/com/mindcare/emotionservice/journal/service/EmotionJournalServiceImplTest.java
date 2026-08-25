package com.mindcare.emotionservice.journal.service;

import com.mindcare.emotionservice.journal.dto.CreateEmotionJournalRequest;
import com.mindcare.emotionservice.journal.dto.EmotionJournalResponse;
import com.mindcare.emotionservice.journal.dto.EmotionTrendPointResponse;
import com.mindcare.emotionservice.journal.entity.EmotionJournalEntity;
import com.mindcare.emotionservice.journal.entity.EmotionType;
import com.mindcare.emotionservice.journal.exception.JournalDeletionWindowExpiredException;
import com.mindcare.emotionservice.journal.mapper.EmotionJournalMapper;
import com.mindcare.emotionservice.journal.repository.EmotionJournalRepository;
import com.mindcare.emotionservice.shared.exception.InvalidRequestException;
import com.mindcare.emotionservice.shared.util.CursorCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmotionJournalServiceImplTest {

    @Mock
    private EmotionJournalRepository repository;
    @Mock
    private EmotionJournalMapper mapper;

    private EmotionJournalServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EmotionJournalServiceImpl(
                repository,
                mapper,
                new CursorCodec(),
                Clock.fixed(Instant.parse("2026-07-22T00:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void createJournalNormalizesContentBeforePersistence() {
        UUID userId = UUID.randomUUID();
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(new EmotionJournalResponse(
                null,
                EmotionType.HAPPY,
                "hello\nworld",
                null,
                null
        ));

        service.createJournal(userId, new CreateEmotionJournalRequest(EmotionType.HAPPY, "  hello\r\nworld  "));

        ArgumentCaptor<EmotionJournalEntity> captor = ArgumentCaptor.forClass(EmotionJournalEntity.class);
        verify(repository).saveAndFlush(captor.capture());
        assertEquals("hello\nworld", captor.getValue().getContent());
    }

    @Test
    void createJournalStoresBlankContentAsNull() {
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(new EmotionJournalResponse(null, EmotionType.NEUTRAL, null, null, null));

        service.createJournal(UUID.randomUUID(), new CreateEmotionJournalRequest(EmotionType.NEUTRAL, "   "));

        ArgumentCaptor<EmotionJournalEntity> captor = ArgumentCaptor.forClass(EmotionJournalEntity.class);
        verify(repository).saveAndFlush(captor.capture());
        assertNull(captor.getValue().getContent());
    }

    @Test
    void createJournalPersistsOptionalDailyCheckInSignals() {
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(new EmotionJournalResponse(
                null, EmotionType.HAPPY, null, 4, 2, 5, null, null));

        service.createJournal(UUID.randomUUID(),
                new CreateEmotionJournalRequest(EmotionType.HAPPY, null, 4, 2, 5));

        ArgumentCaptor<EmotionJournalEntity> captor = ArgumentCaptor.forClass(EmotionJournalEntity.class);
        verify(repository).saveAndFlush(captor.capture());
        assertEquals(4, captor.getValue().getEnergyLevel());
        assertEquals(2, captor.getValue().getStressLevel());
        assertEquals(5, captor.getValue().getSleepQuality());
    }

    @Test
    void createJournalRejectsMissingEmotion() {
        assertThrows(
                InvalidRequestException.class,
                () -> service.createJournal(UUID.randomUUID(), new CreateEmotionJournalRequest(null, "content"))
        );
    }

    @Test
    void deleteJournalAllowsDeletionAtExactlyFifteenMinuteBoundary() {
        UUID userId = UUID.randomUUID();
        UUID journalId = UUID.randomUUID();
        EmotionJournalEntity entity =
                new EmotionJournalEntity(userId, EmotionType.NEUTRAL, "content");
        ReflectionTestUtils.setField(
                entity,
                "createdAt",
                OffsetDateTime.parse("2026-07-21T23:45:00Z")
        );
        when(repository.findByIdAndUserIdAndDeletedAtIsNull(journalId, userId))
                .thenReturn(Optional.of(entity));

        service.deleteJournal(userId, journalId);

        assertEquals(
                OffsetDateTime.parse("2026-07-22T00:00:00Z"),
                entity.getDeletedAt()
        );
        verify(repository).save(entity);
    }

    @Test
    void deleteJournalRejectsDeletionAfterFifteenMinuteWindow() {
        UUID userId = UUID.randomUUID();
        UUID journalId = UUID.randomUUID();
        EmotionJournalEntity entity =
                new EmotionJournalEntity(userId, EmotionType.NEUTRAL, "content");
        ReflectionTestUtils.setField(
                entity,
                "createdAt",
                OffsetDateTime.parse("2026-07-21T23:44:59.999Z")
        );
        when(repository.findByIdAndUserIdAndDeletedAtIsNull(journalId, userId))
                .thenReturn(Optional.of(entity));

        JournalDeletionWindowExpiredException exception = assertThrows(
                JournalDeletionWindowExpiredException.class,
                () -> service.deleteJournal(userId, journalId)
        );

        assertEquals("JOURNAL_DELETION_WINDOW_EXPIRED", exception.getCode());
        assertNull(entity.getDeletedAt());
        verify(repository, never()).save(any());
    }

    @Test
    void getEmotionTrendsReturnsContinuousDailyBucketsWithVersionedAverages() {
        UUID userId = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.parse("2026-07-20T00:00:00+07:00");
        OffsetDateTime to = OffsetDateTime.parse("2026-07-27T00:00:00+07:00");
        EmotionJournalEntity monday =
                journalAt(userId, EmotionType.HAPPY, "2026-07-20T08:00:00+07:00");
        EmotionJournalEntity wednesdayPositive =
                journalAt(userId, EmotionType.VERY_HAPPY, "2026-07-22T09:00:00+07:00");
        EmotionJournalEntity wednesdayNegative =
                journalAt(userId, EmotionType.STRESSED, "2026-07-22T20:00:00+07:00");
        when(repository
                .findByUserIdAndDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(
                        userId,
                        from,
                        to
                ))
                .thenReturn(List.of(monday, wednesdayPositive, wednesdayNegative));

        List<EmotionTrendPointResponse> result = service.getEmotionTrends(
                userId,
                from,
                to,
                "DAY",
                ZoneId.of("Asia/Ho_Chi_Minh")
        );

        assertEquals(7, result.size());
        assertEquals(OffsetDateTime.parse("2026-07-20T00:00:00+07:00"), result.get(0).periodStart());
        assertEquals(new BigDecimal("1.00"), result.get(0).averageScore());
        assertEquals(1, result.get(0).count());
        assertEquals("emotion-v1", result.get(0).mappingVersion());
        assertNull(result.get(1).averageScore());
        assertEquals(0, result.get(1).count());
        assertEquals(new BigDecimal("0.00"), result.get(2).averageScore());
        assertEquals(2, result.get(2).count());
        assertEquals(OffsetDateTime.parse("2026-07-27T00:00:00+07:00"), result.get(6).periodEnd());
    }

    @Test
    void getEmotionTrendsRejectsUnsupportedBucket() {
        InvalidRequestException exception = assertThrows(
                InvalidRequestException.class,
                () -> service.getEmotionTrends(
                        UUID.randomUUID(),
                        OffsetDateTime.parse("2026-07-20T00:00:00Z"),
                        OffsetDateTime.parse("2026-07-27T00:00:00Z"),
                        "YEAR",
                        ZoneOffset.UTC
                )
        );

        assertEquals("INVALID_BUCKET", exception.getCode());
        verify(repository, never())
                .findByUserIdAndDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(
                        any(),
                        any(),
                        any()
                );
    }

    private EmotionJournalEntity journalAt(
            UUID userId,
            EmotionType emotionType,
            String createdAt
    ) {
        EmotionJournalEntity entity =
                new EmotionJournalEntity(userId, emotionType, null);
        ReflectionTestUtils.setField(
                entity,
                "createdAt",
                OffsetDateTime.parse(createdAt)
        );
        return entity;
    }
}
