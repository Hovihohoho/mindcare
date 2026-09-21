package com.mindcare.emotionservice.selfcare.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(name = "self_care_activities", schema = "emotion_schema")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SelfCareActivityEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "plan_id") private SelfCarePlanEntity plan;
    @Column(name = "activity_code", nullable = false, length = 50) private String activityCode;
    @Column(nullable = false, length = 160) private String title;
    @Column(name = "target_per_week", nullable = false) private int targetPerWeek;
    @Column(name = "display_order", nullable = false) private int displayOrder;
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @LastModifiedDate @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;

    public SelfCareActivityEntity(String code, String title, int targetPerWeek, int displayOrder) {
        this.activityCode = code; this.title = title; this.targetPerWeek = targetPerWeek; this.displayOrder = displayOrder;
    }
    public void attachTo(SelfCarePlanEntity value) { this.plan = value; }
}
