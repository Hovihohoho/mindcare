package com.mindcare.emotionservice.selfcare.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(name = "self_care_completions", schema = "emotion_schema")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SelfCareCompletionEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "activity_id") private SelfCareActivityEntity activity;
    @Column(name = "completed_on", nullable = false) private LocalDate completedOn;
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    public SelfCareCompletionEntity(SelfCareActivityEntity activity, LocalDate completedOn) { this.activity = activity; this.completedOn = completedOn; }
}
