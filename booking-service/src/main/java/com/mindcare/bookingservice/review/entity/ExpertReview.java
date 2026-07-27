package com.mindcare.bookingservice.review.entity;

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
@Table(name = "expert_reviews", schema = "booking_schema")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpertReview extends AuditableEntity {

    @Column(name = "booking_id", nullable = false, updatable = false)
    private UUID bookingId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "expert_user_id", nullable = false, updatable = false)
    private UUID expertUserId;

    @Column(nullable = false)
    private short rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    public static ExpertReview create(
            UUID bookingId,
            UUID userId,
            UUID expertUserId,
            short rating,
            String comment) {
        ExpertReview review = new ExpertReview();
        review.bookingId = bookingId;
        review.userId = userId;
        review.expertUserId = expertUserId;
        review.rating = rating;
        review.comment = comment == null || comment.isBlank() ? null : comment.trim();
        return review;
    }
}
