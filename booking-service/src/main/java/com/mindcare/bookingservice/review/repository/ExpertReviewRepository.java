package com.mindcare.bookingservice.review.repository;

import com.mindcare.bookingservice.review.entity.ExpertReview;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpertReviewRepository extends JpaRepository<ExpertReview, UUID> {

    Optional<ExpertReview> findByBookingIdAndDeletedAtIsNull(UUID bookingId);

    @Query("""
            SELECT review
            FROM ExpertReview review
            WHERE review.expertUserId = :expertUserId
              AND review.deletedAt IS NULL
              AND (:cursorCreatedAt IS NULL
                    OR review.createdAt < :cursorCreatedAt
                    OR (review.createdAt = :cursorCreatedAt AND review.id < :cursorId))
            ORDER BY review.createdAt DESC, review.id DESC
            """)
    List<ExpertReview> findPublicHistory(
            @Param("expertUserId") UUID expertUserId,
            @Param("cursorCreatedAt") OffsetDateTime cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable);

    @Query("""
            SELECT AVG(review.rating) AS ratingAverage,
                   COUNT(review.id) AS reviewCount
            FROM ExpertReview review
            WHERE review.expertUserId = :expertUserId
              AND review.deletedAt IS NULL
            """)
    RatingAggregate aggregateForExpert(@Param("expertUserId") UUID expertUserId);
}
