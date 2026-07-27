package com.mindcare.emotionservice.assessment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "assessments", schema = "emotion_schema")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssessmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Convert(converter = AssessmentCodeJpaConverter.class)
    @Column(name = "code", nullable = false, length = 50)
    private AssessmentCode code;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "assessment_version", nullable = false)
    private Integer assessmentVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AssessmentStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public AssessmentEntity(AssessmentCode code, String title, String description) {
        this(code, title, description, 1);
    }

    public AssessmentEntity(
            AssessmentCode code,
            String title,
            String description,
            Integer assessmentVersion
    ) {
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.description = description;
        this.assessmentVersion = Objects.requireNonNull(assessmentVersion, "assessmentVersion must not be null");
        this.status = AssessmentStatus.DRAFT;
    }

    public void changeTitle(String title) {
        this.title = Objects.requireNonNull(title, "title must not be null");
    }

    public void changeDescription(String description) {
        this.description = description;
    }

    public void softDelete(OffsetDateTime deletedAt) {
        this.deletedAt = Objects.requireNonNull(deletedAt, "deletedAt must not be null");
    }

    public void publish() {
        this.status = AssessmentStatus.PUBLISHED;
    }

    public void archive() {
        this.status = AssessmentStatus.ARCHIVED;
    }
}
