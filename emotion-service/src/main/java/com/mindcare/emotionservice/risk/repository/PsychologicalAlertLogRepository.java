package com.mindcare.emotionservice.risk.repository;

import com.mindcare.emotionservice.risk.entity.PsychologicalAlertLogEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PsychologicalAlertLogRepository extends JpaRepository<PsychologicalAlertLogEntity, UUID> {

    Optional<PsychologicalAlertLogEntity> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    Optional<PsychologicalAlertLogEntity> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByUserIdAndDeduplicationKeyAndDeletedAtIsNullAndCreatedAtGreaterThanEqual(
            UUID userId,
            String deduplicationKey,
            OffsetDateTime createdAfter
    );

    @Query("""
            SELECT alert FROM PsychologicalAlertLogEntity alert
            WHERE alert.userId = :userId
              AND alert.deletedAt IS NULL
              AND alert.createdAt >= :from
              AND alert.createdAt < :to
              AND (:hasCursor = FALSE
                   OR alert.createdAt < :cursorCreatedAt
                   OR (alert.createdAt = :cursorCreatedAt AND alert.id < :cursorId))
            ORDER BY alert.createdAt DESC, alert.id DESC
            """)
    List<PsychologicalAlertLogEntity> findHistory(
            @Param("userId") UUID userId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("hasCursor") boolean hasCursor,
            @Param("cursorCreatedAt") OffsetDateTime cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );
}
