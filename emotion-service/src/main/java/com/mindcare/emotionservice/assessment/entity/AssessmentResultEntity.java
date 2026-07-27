package com.mindcare.emotionservice.assessment.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
@Entity
@Table(name = "assessment_results", schema = "emotion_schema")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssessmentResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false, updatable = false)
    private AssessmentEntity assessment;

    @Column(name = "total_score", nullable = false, updatable = false)
    private Integer totalScore;

    @Column(name = "risk_level", nullable = false, updatable = false, length = 50)
    private String riskLevel;

    @Column(name = "assessment_version", nullable = false, updatable = false)
    private Integer assessmentVersion;

    @Column(name = "scoring_rule_version", nullable = false, updatable = false, length = 50)
    private String scoringRuleVersion;

    @Column(name = "idempotency_key", updatable = false, length = 255)
    private String idempotencyKey;

    @Column(name = "submission_hash", updatable = false, length = 64)
    private String submissionHash;

    @Column(name = "screening_notice", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String screeningNotice;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recommendations", nullable = false, updatable = false, columnDefinition = "JSONB")
    private JsonNode recommendations;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answers_detail", columnDefinition = "JSONB", updatable = false)
    private JsonNode answersDetail;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public AssessmentResultEntity(
            UUID userId,
            AssessmentEntity assessment,
            Integer totalScore,
            String riskLevel,
            JsonNode answersDetail,
            Integer assessmentVersion,
            String scoringRuleVersion,
            String idempotencyKey,
            String submissionHash,
            String screeningNotice,
            JsonNode recommendations
    ) {
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
        this.assessment = Objects.requireNonNull(assessment, "assessment must not be null");
        this.totalScore = Objects.requireNonNull(totalScore, "totalScore must not be null");
        this.riskLevel = Objects.requireNonNull(riskLevel, "riskLevel must not be null");
        this.answersDetail = answersDetail;
        this.assessmentVersion = Objects.requireNonNull(assessmentVersion, "assessmentVersion must not be null");
        this.scoringRuleVersion = Objects.requireNonNull(scoringRuleVersion, "scoringRuleVersion must not be null");
        this.idempotencyKey = idempotencyKey;
        this.submissionHash = submissionHash;
        this.screeningNotice = Objects.requireNonNull(screeningNotice, "screeningNotice must not be null");
        this.recommendations = Objects.requireNonNull(recommendations, "recommendations must not be null");
    }

    public void softDelete(OffsetDateTime deletedAt) {
        this.deletedAt = Objects.requireNonNull(deletedAt, "deletedAt must not be null");
    }
}
