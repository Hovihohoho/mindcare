package com.mindcare.emotionservice.stressprediction.entity;

import com.mindcare.emotionservice.stressprediction.dto.StoreStressPredictionRequest;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "pmdata_stress_predictions", schema = "emotion_schema")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PmdataStressPredictionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;
    @Column(name = "feature_date", nullable = false, updatable = false)
    private LocalDate featureDate;
    @Column(nullable = false, length = 80)
    private String timezone;
    @Column(name = "feature_version", nullable = false, length = 60)
    private String featureVersion;
    @Column(name = "stress_score", nullable = false)
    private int stressScore;
    @Column(name = "relative_level", nullable = false, length = 30)
    private String relativeLevel;
    @Column(nullable = false, precision = 6, scale = 5)
    private BigDecimal confidence;
    @Column(name = "model_version", nullable = false, updatable = false, length = 80)
    private String modelVersion;
    @Column(name = "alert_level", nullable = false, length = 30)
    private String alertLevel;
    @Column(name = "notification_required", nullable = false)
    private boolean notificationRequired;
    @Column(name = "notified_score")
    private Integer notifiedScore;
    @Column(name = "notified_at")
    private OffsetDateTime notifiedAt;
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public PmdataStressPredictionEntity(UUID userId, StoreStressPredictionRequest request) {
        this.userId = userId;
        this.featureDate = request.featureDate();
        this.modelVersion = request.modelVersion();
        update(request);
    }

    public void update(StoreStressPredictionRequest request) {
        timezone = request.timezone();
        featureVersion = request.featureVersion();
        stressScore = request.stressScore();
        relativeLevel = stressScore < 3 ? "BELOW_NORMAL" : stressScore == 3 ? "NORMAL" : "ABOVE_NORMAL";
        confidence = request.confidence();
        notificationRequired = stressScore >= 4;
        alertLevel = stressScore == 5 ? "HIGH" : stressScore == 4 ? "ELEVATED" : stressScore == 3 ? "MONITOR" : "INFORMATIONAL";
    }

    public boolean requiresNewNotification() {
        return notificationRequired && (notifiedScore == null || stressScore > notifiedScore);
    }

    public void markNotified(int score, OffsetDateTime at) {
        notifiedScore = score;
        notifiedAt = at;
    }
}
