package com.mindcare.emotionservice.assessment.repository;

import com.mindcare.emotionservice.assessment.entity.AssessmentResultEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssessmentResultRepository extends JpaRepository<AssessmentResultEntity, UUID> {

    Optional<AssessmentResultEntity> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    Optional<AssessmentResultEntity> findByUserIdAndAssessment_IdAndIdempotencyKeyAndDeletedAtIsNull(
            UUID userId,
            UUID assessmentId,
            String idempotencyKey
    );

    @Query("""
            SELECT result FROM AssessmentResultEntity result
            WHERE result.userId = :userId
              AND result.deletedAt IS NULL
              AND result.createdAt >= :from
              AND result.createdAt < :to
              AND (:hasCursor = FALSE
                   OR result.createdAt < :cursorCreatedAt
                   OR (result.createdAt = :cursorCreatedAt AND result.id < :cursorId))
            ORDER BY result.createdAt DESC, result.id DESC
            """)
    List<AssessmentResultEntity> findHistory(
            @Param("userId") UUID userId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("hasCursor") boolean hasCursor,
            @Param("cursorCreatedAt") OffsetDateTime cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );
}
