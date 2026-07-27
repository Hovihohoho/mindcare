package com.mindcare.emotionservice.journal.service;

import com.mindcare.emotionservice.journal.dto.CreateEmotionJournalRequest;
import com.mindcare.emotionservice.journal.dto.EmotionJournalResponse;
import com.mindcare.emotionservice.journal.dto.EmotionTrendPointResponse;
import com.mindcare.emotionservice.journal.entity.EmotionJournalEntity;
import com.mindcare.emotionservice.journal.entity.EmotionType;
import com.mindcare.emotionservice.journal.exception.JournalDeletionWindowExpiredException;
import com.mindcare.emotionservice.journal.mapper.EmotionJournalMapper;
import com.mindcare.emotionservice.journal.repository.EmotionJournalRepository;
import com.mindcare.emotionservice.shared.dto.CursorPageResponse;
import com.mindcare.emotionservice.shared.exception.InvalidRequestException;
import com.mindcare.emotionservice.shared.exception.ResourceNotFoundException;
import com.mindcare.emotionservice.shared.util.CursorCodec;
import com.mindcare.emotionservice.shared.util.ServiceValidator;
import com.mindcare.emotionservice.shared.util.TrendBucket;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class EmotionJournalServiceImpl implements EmotionJournalService {

    private static final int MAX_CONTENT_CODE_POINTS = 5_000;
    private static final int DELETION_WINDOW_MINUTES = 15;
    private static final String MAPPING_VERSION = "emotion-v1";
    private static final Map<EmotionType, Integer> EMOTION_SCORES = emotionScores();

    private final EmotionJournalRepository repository;
    private final EmotionJournalMapper mapper;
    private final CursorCodec cursorCodec;
    private final Clock clock;

    public EmotionJournalServiceImpl(
            EmotionJournalRepository repository,
            EmotionJournalMapper mapper,
            CursorCodec cursorCodec,
            Clock clock
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.cursorCodec = cursorCodec;
        this.clock = clock;
    }

    @Override
    @Transactional
    public EmotionJournalResponse createJournal(UUID userId, CreateEmotionJournalRequest request) {
        ServiceValidator.requireUserId(userId);
        if (request == null || request.emotionType() == null) {
            throw new InvalidRequestException("INVALID_REQUEST", "emotionType must not be null");
        }
        EmotionJournalEntity entity = new EmotionJournalEntity(
                userId,
                request.emotionType(),
                normalizeContent(request.content())
        );
        return mapper.toResponse(repository.saveAndFlush(entity));
    }

    @Override
    public EmotionJournalResponse getJournal(UUID userId, UUID journalId) {
        return mapper.toResponse(findOwnedJournal(userId, journalId));
    }

    @Override
    public CursorPageResponse<EmotionJournalResponse> getJournalHistory(
            UUID userId,
            OffsetDateTime from,
            OffsetDateTime to,
            String cursor,
            int limit
    ) {
        ServiceValidator.requireUserId(userId);
        ServiceValidator.validateTimeRange(from, to, 365);
        ServiceValidator.validateLimit(limit);
        String scope = "journal:" + userId + ':' + from + ':' + to;
        CursorCodec.CursorPosition position = cursorCodec.decode(cursor, scope);
        List<EmotionJournalEntity> entities = repository.findHistory(
                userId,
                from,
                to,
                position.timestamp() != null,
                position.timestamp(),
                position.id(),
                PageRequest.of(0, limit + 1)
        );
        boolean hasMore = entities.size() > limit;
        List<EmotionJournalEntity> page = entities.subList(0, Math.min(limit, entities.size()));
        String nextCursor = hasMore
                ? cursorCodec.encode(page.get(page.size() - 1).getCreatedAt(), page.get(page.size() - 1).getId(), scope)
                : null;
        return new CursorPageResponse<>(page.stream().map(mapper::toResponse).toList(), nextCursor, hasMore);
    }

    @Override
    @Transactional
    public void deleteJournal(UUID userId, UUID journalId) {
        EmotionJournalEntity entity = findOwnedJournal(userId, journalId);
        OffsetDateTime deletedAt = OffsetDateTime.now(clock);
        if (deletedAt.isAfter(entity.getCreatedAt().plusMinutes(DELETION_WINDOW_MINUTES))) {
            throw new JournalDeletionWindowExpiredException();
        }
        entity.softDelete(deletedAt);
        repository.save(entity);
    }

    @Override
    public List<EmotionTrendPointResponse> getEmotionTrends(
            UUID userId,
            OffsetDateTime from,
            OffsetDateTime to,
            String bucket,
            ZoneId timezone
    ) {
        ServiceValidator.requireUserId(userId);
        ServiceValidator.validateTimeRange(from, to, 730);
        if (timezone == null) {
            throw new InvalidRequestException("INVALID_TIMEZONE", "timezone must not be null");
        }
        TrendBucket trendBucket = TrendBucket.parse(bucket);
        List<EmotionJournalEntity> journals = repository
                .findByUserIdAndDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(
                        userId,
                        from,
                        to
                );
        Map<OffsetDateTime, ScoreAccumulator> groups = new LinkedHashMap<>();
        for (EmotionJournalEntity journal : journals) {
            OffsetDateTime periodStart = trendBucket.startOf(journal.getCreatedAt(), timezone);
            groups.computeIfAbsent(periodStart, ignored -> new ScoreAccumulator())
                    .add(EMOTION_SCORES.get(journal.getEmotionType()));
        }
        List<EmotionTrendPointResponse> response = new ArrayList<>();
        OffsetDateTime periodStart = trendBucket.startOf(from, timezone);
        while (periodStart.toInstant().isBefore(to.toInstant())) {
            ScoreAccumulator accumulator = groups.get(periodStart);
            response.add(new EmotionTrendPointResponse(
                    periodStart,
                    trendBucket.endOf(periodStart, timezone),
                    accumulator == null ? null : accumulator.average(),
                    accumulator == null ? 0 : accumulator.count,
                    MAPPING_VERSION
            ));
            periodStart = trendBucket.endOf(periodStart, timezone);
        }
        return List.copyOf(response);
    }

    private EmotionJournalEntity findOwnedJournal(UUID userId, UUID journalId) {
        ServiceValidator.requireUserId(userId);
        if (journalId == null) {
            throw new InvalidRequestException("INVALID_REQUEST", "journalId must not be null");
        }
        return repository.findByIdAndUserIdAndDeletedAtIsNull(journalId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Emotion journal"));
    }

    private String normalizeContent(String content) {
        if (content == null) {
            return null;
        }
        String normalized = content.replace("\r\n", "\n").replace('\r', '\n').trim();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.codePointCount(0, normalized.length()) > MAX_CONTENT_CODE_POINTS) {
            throw new InvalidRequestException(
                    "JOURNAL_CONTENT_TOO_LONG",
                    "content must not exceed " + MAX_CONTENT_CODE_POINTS + " characters"
            );
        }
        return normalized;
    }

    private static Map<EmotionType, Integer> emotionScores() {
        EnumMap<EmotionType, Integer> scores = new EnumMap<>(EmotionType.class);
        scores.put(EmotionType.VERY_HAPPY, 2);
        scores.put(EmotionType.HAPPY, 1);
        scores.put(EmotionType.NEUTRAL, 0);
        scores.put(EmotionType.SAD, -1);
        scores.put(EmotionType.STRESSED, -2);
        return Map.copyOf(scores);
    }

    private static final class ScoreAccumulator {
        private long sum;
        private long count;

        void add(int score) {
            sum += score;
            count++;
        }

        BigDecimal average() {
            return BigDecimal.valueOf(sum)
                    .divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        }
    }
}
