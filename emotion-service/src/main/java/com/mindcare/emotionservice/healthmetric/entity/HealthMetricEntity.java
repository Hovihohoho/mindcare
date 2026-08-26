package com.mindcare.emotionservice.healthmetric.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(name = "health_metrics", schema = "emotion_schema")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HealthMetricEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "metric_type", nullable = false, length = 50)
    private String metricType;

    @Column(name = "metric_value", precision = 10, scale = 2)
    private BigDecimal metricValue;

    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(name = "external_sample_id", length = 255)
    private String externalSampleId;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    @Column(name = "start_time")
    private OffsetDateTime startTime;

    @Column(name = "end_time")
    private OffsetDateTime endTime;

    @Column(name = "source_name", length = 255)
    private String sourceName;

    @Column(name = "data_origin", length = 255)
    private String dataOrigin;

    @Column(name = "source_last_modified_at")
    private OffsetDateTime sourceLastModifiedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "record_details", columnDefinition = "JSONB")
    private Map<String, Object> details;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public HealthMetricEntity(
            UUID userId,
            String metricType,
            BigDecimal metricValue,
            String unit,
            String sourceType,
            String externalSampleId,
            OffsetDateTime recordedAt
    ) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.metricType = Objects.requireNonNull(metricType, "metricType must not be null");
        this.metricValue = Objects.requireNonNull(metricValue, "metricValue must not be null");
        this.unit = unit;
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType must not be null");
        this.externalSampleId = externalSampleId;
        this.recordedAt = Objects.requireNonNull(recordedAt, "recordedAt must not be null");
        this.details = Map.of();
    }

    public HealthMetricEntity(
            UUID userId,
            String metricType,
            BigDecimal metricValue,
            String unit,
            String sourceType,
            String externalSampleId,
            OffsetDateTime recordedAt,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            String sourceName,
            String dataOrigin,
            OffsetDateTime sourceLastModifiedAt,
            Map<String, Object> details
    ) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.sourceType = Objects.requireNonNull(sourceType, "sourceType must not be null");
        this.externalSampleId = externalSampleId;
        applyUpsert(metricType, metricValue, unit, recordedAt, startTime, endTime,
                sourceName, dataOrigin, sourceLastModifiedAt, details);
    }

    public void applyUpsert(
            String metricType,
            BigDecimal metricValue,
            String unit,
            OffsetDateTime recordedAt,
            OffsetDateTime startTime,
            OffsetDateTime endTime,
            String sourceName,
            String dataOrigin,
            OffsetDateTime sourceLastModifiedAt,
            Map<String, Object> details
    ) {
        this.metricType = Objects.requireNonNull(metricType, "metricType must not be null");
        this.metricValue = metricValue;
        this.unit = unit;
        this.recordedAt = Objects.requireNonNull(recordedAt, "recordedAt must not be null");
        this.startTime = startTime;
        this.endTime = endTime;
        this.sourceName = sourceName;
        this.dataOrigin = dataOrigin;
        this.sourceLastModifiedAt = sourceLastModifiedAt;
        this.details = details == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(details));
        this.deletedAt = null;
    }

    public void softDelete(OffsetDateTime deletedAt) {
        this.deletedAt = Objects.requireNonNull(deletedAt, "deletedAt must not be null");
    }
}
