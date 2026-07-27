package com.mindcare.emotionservice.journal.repository;

import com.mindcare.emotionservice.journal.entity.EmotionJournalEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmotionJournalRepository extends JpaRepository<EmotionJournalEntity, UUID> {

    Optional<EmotionJournalEntity> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    @Query("""
            SELECT journal FROM EmotionJournalEntity journal
            WHERE journal.userId = :userId
              AND journal.deletedAt IS NULL
              AND journal.createdAt >= :from
              AND journal.createdAt < :to
              AND (:hasCursor = FALSE
                   OR journal.createdAt < :cursorCreatedAt
                   OR (journal.createdAt = :cursorCreatedAt AND journal.id < :cursorId))
            ORDER BY journal.createdAt DESC, journal.id DESC
            """)
    List<EmotionJournalEntity> findHistory(
            @Param("userId") UUID userId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("hasCursor") boolean hasCursor,
            @Param("cursorCreatedAt") OffsetDateTime cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    List<EmotionJournalEntity> findByUserIdAndDeletedAtIsNullAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtAsc(
            UUID userId,
            OffsetDateTime from,
            OffsetDateTime to
    );
}
