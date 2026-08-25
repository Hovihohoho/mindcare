package com.mindcare.emotionservice.selfcare.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(name = "self_care_plans", schema = "emotion_schema")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SelfCarePlanEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "user_id", nullable = false, updatable = false) private UUID userId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private SelfCareGoal goal;
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<SelfCareActivityEntity> activities = new ArrayList<>();
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @LastModifiedDate @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;

    public SelfCarePlanEntity(UUID userId, SelfCareGoal goal) { this.userId = userId; this.goal = goal; }
    public void replace(SelfCareGoal goal, List<SelfCareActivityEntity> values) {
        this.goal = goal;
        activities.clear();
        values.forEach(value -> { value.attachTo(this); activities.add(value); });
    }
}
