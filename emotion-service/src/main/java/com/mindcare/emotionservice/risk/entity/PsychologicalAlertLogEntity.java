package com.mindcare.emotionservice.risk.entity;

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

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(name = "psychological_alert_logs", schema = "emotion_schema")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PsychologicalAlertLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "alert_level", nullable = false, updatable = false, length = 50)
    private String alertLevel;

    @Column(name = "trigger_reason", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String triggerReason;

    @Column(name = "is_notified")
    private Boolean notified;

    @Column(name = "rule_version", nullable = false, updatable = false, length = 50)
    private String ruleVersion;

    @Column(name = "reason_code", nullable = false, updatable = false, length = 100)
    private String reasonCode;

    @Column(name = "source_result_id", updatable = false)
    private UUID sourceResultId;

    @Column(name = "deduplication_key", updatable = false, length = 255)
    private String deduplicationKey;

    @Column(name = "notified_at")
    private OffsetDateTime notifiedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public PsychologicalAlertLogEntity(
            UUID userId,
            String alertLevel,
            String triggerReason,
            String ruleVersion,
            String reasonCode,
            UUID sourceResultId,
            String deduplicationKey
    ) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.alertLevel = Objects.requireNonNull(alertLevel, "alertLevel must not be null");
        this.triggerReason = Objects.requireNonNull(triggerReason, "triggerReason must not be null");
        this.ruleVersion = Objects.requireNonNull(ruleVersion, "ruleVersion must not be null");
        this.reasonCode = Objects.requireNonNull(reasonCode, "reasonCode must not be null");
        this.sourceResultId = sourceResultId;
        this.deduplicationKey = deduplicationKey;
        this.notified = false;
    }

    public void markNotified(OffsetDateTime notifiedAt) {
        this.notified = true;
        this.notifiedAt = Objects.requireNonNull(notifiedAt, "notifiedAt must not be null");
    }

    public void softDelete(OffsetDateTime deletedAt) {
        this.deletedAt = Objects.requireNonNull(deletedAt, "deletedAt must not be null");
    }
}
