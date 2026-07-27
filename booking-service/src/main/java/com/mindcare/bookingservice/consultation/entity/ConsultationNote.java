package com.mindcare.bookingservice.consultation.entity;

import com.mindcare.bookingservice.shared.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "consultation_notes", schema = "booking_schema")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsultationNote extends AuditableEntity {

    @Column(name = "booking_id", nullable = false, updatable = false)
    private UUID bookingId;

    @Column(name = "expert_user_id", nullable = false, updatable = false)
    private UUID expertUserId;

    @Column(columnDefinition = "TEXT")
    private String observation;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String recommendation;

    @Column(name = "recovery_plan", columnDefinition = "TEXT")
    private String recoveryPlan;

    @Column(name = "visible_to_user", nullable = false)
    private boolean visibleToUser;

    public static ConsultationNote create(
            UUID bookingId,
            UUID expertUserId,
            String observation,
            String recommendation,
            String recoveryPlan,
            boolean visibleToUser) {
        ConsultationNote note = new ConsultationNote();
        note.bookingId = bookingId;
        note.expertUserId = expertUserId;
        note.update(observation, recommendation, recoveryPlan, visibleToUser);
        return note;
    }

    public void update(
            String observation,
            String recommendation,
            String recoveryPlan,
            boolean visibleToUser) {
        this.observation = normalize(observation);
        this.recommendation = recommendation.trim();
        this.recoveryPlan = normalize(recoveryPlan);
        this.visibleToUser = visibleToUser;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
