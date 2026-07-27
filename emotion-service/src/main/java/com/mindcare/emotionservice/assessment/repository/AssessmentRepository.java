package com.mindcare.emotionservice.assessment.repository;

import com.mindcare.emotionservice.assessment.entity.AssessmentEntity;
import com.mindcare.emotionservice.assessment.entity.AssessmentCode;
import com.mindcare.emotionservice.assessment.entity.AssessmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssessmentRepository extends JpaRepository<AssessmentEntity, UUID> {

    Optional<AssessmentEntity> findByIdAndDeletedAtIsNull(UUID id);

    Optional<AssessmentEntity> findByCodeAndAssessmentVersionAndDeletedAtIsNull(
            AssessmentCode code,
            Integer assessmentVersion
    );

    Optional<AssessmentEntity> findByCodeAndStatusAndDeletedAtIsNull(
            AssessmentCode code,
            AssessmentStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AssessmentEntity> findFirstByCodeAndDeletedAtIsNullOrderByAssessmentVersionDesc(
            AssessmentCode code
    );

    boolean existsByCodeAndStatusAndDeletedAtIsNull(
            AssessmentCode code,
            AssessmentStatus status
    );

    boolean existsByIdAndStatusAndDeletedAtIsNull(
            UUID id,
            AssessmentStatus status
    );

    List<AssessmentEntity> findByStatusAndDeletedAtIsNullOrderByCodeAsc(AssessmentStatus status);

    @Query("""
            SELECT assessment FROM AssessmentEntity assessment
            WHERE assessment.deletedAt IS NULL
              AND (:filterByStatus = FALSE OR assessment.status = :status)
              AND (:hasCursor = FALSE
                   OR assessment.createdAt < :cursorCreatedAt
                   OR (assessment.createdAt = :cursorCreatedAt AND assessment.id < :cursorId))
            ORDER BY assessment.createdAt DESC, assessment.id DESC
            """)
    List<AssessmentEntity> findForAdmin(
            @Param("filterByStatus") boolean filterByStatus,
            @Param("status") AssessmentStatus status,
            @Param("hasCursor") boolean hasCursor,
            @Param("cursorCreatedAt") OffsetDateTime cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );
}
