package com.mindcare.emotionservice.healthmetric.repository;

import com.mindcare.emotionservice.healthmetric.entity.HealthMetricEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface HealthMetricRepository extends JpaRepository<HealthMetricEntity, UUID> {

    boolean existsByUserIdAndSourceTypeAndExternalSampleIdAndDeletedAtIsNull(
            UUID userId,
            String sourceType,
            String externalSampleId
    );

    Optional<HealthMetricEntity> findByUserIdAndSourceTypeAndExternalSampleId(
            UUID userId,
            String sourceType,
            String externalSampleId
    );

    @Query("""
            SELECT metric FROM HealthMetricEntity metric
            WHERE metric.userId = :userId
              AND metric.metricType = :metricType
              AND metric.deletedAt IS NULL
              AND metric.recordedAt >= :from
              AND metric.recordedAt < :to
              AND (:hasCursor = FALSE
                   OR metric.recordedAt < :cursorRecordedAt
                   OR (metric.recordedAt = :cursorRecordedAt AND metric.id < :cursorId))
            ORDER BY metric.recordedAt DESC, metric.id DESC
            """)
    List<HealthMetricEntity> findHistory(
            @Param("userId") UUID userId,
            @Param("metricType") String metricType,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to,
            @Param("hasCursor") boolean hasCursor,
            @Param("cursorRecordedAt") OffsetDateTime cursorRecordedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    List<HealthMetricEntity> findByUserIdAndMetricTypeAndDeletedAtIsNullAndRecordedAtGreaterThanEqualAndRecordedAtLessThanOrderByRecordedAtAsc(
            UUID userId,
            String metricType,
            OffsetDateTime from,
            OffsetDateTime to
    );

    long countByUserIdAndSourceTypeAndDeletedAtIsNull(UUID userId, String sourceType);

    @Query("SELECT MIN(metric.recordedAt), MAX(metric.recordedAt) FROM HealthMetricEntity metric WHERE metric.userId = :userId AND metric.sourceType = :sourceType AND metric.deletedAt IS NULL")
    Object[] findActiveRange(@Param("userId") UUID userId, @Param("sourceType") String sourceType);

    @Query("SELECT metric.metricType, COUNT(metric) FROM HealthMetricEntity metric WHERE metric.userId = :userId AND metric.sourceType = :sourceType AND metric.deletedAt IS NULL GROUP BY metric.metricType")
    List<Object[]> countActiveByMetricType(@Param("userId") UUID userId, @Param("sourceType") String sourceType);

    @Modifying
    long deleteByUserIdAndSourceType(UUID userId, String sourceType);
}
