package com.mindcare.bookingservice.booking.repository;

import com.mindcare.bookingservice.booking.entity.Booking;
import com.mindcare.bookingservice.booking.entity.BookingStatus;
import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT booking
            FROM Booking booking
            WHERE booking.id = :id
              AND booking.deletedAt IS NULL
            """)
    Optional<Booking> findActiveByIdForUpdate(@Param("id") UUID id);

    Optional<Booking> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    Optional<Booking> findByIdAndExpertUserIdAndDeletedAtIsNull(UUID id, UUID expertUserId);

    Optional<Booking> findByScheduleIdAndStatusAndDeletedAtIsNull(
            UUID scheduleId,
            BookingStatus status);

    Optional<Booking> findByUserIdAndIdempotencyKeyAndDeletedAtIsNull(
            UUID userId,
            String idempotencyKey);

    @Query("""
            SELECT booking
            FROM Booking booking
            WHERE booking.userId = :userId
              AND booking.deletedAt IS NULL
              AND (:status IS NULL OR booking.status = :status)
              AND (:cursorCreatedAt IS NULL
                    OR booking.createdAt < :cursorCreatedAt
                    OR (booking.createdAt = :cursorCreatedAt AND booking.id < :cursorId))
            ORDER BY booking.createdAt DESC, booking.id DESC
            """)
    List<Booking> findUserHistory(
            @Param("userId") UUID userId,
            @Param("status") BookingStatus status,
            @Param("cursorCreatedAt") OffsetDateTime cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable);

    @Query("""
            SELECT booking
            FROM Booking booking
            WHERE booking.expertUserId = :expertUserId
              AND booking.deletedAt IS NULL
              AND (:status IS NULL OR booking.status = :status)
              AND (:cursorCreatedAt IS NULL
                    OR booking.createdAt < :cursorCreatedAt
                    OR (booking.createdAt = :cursorCreatedAt AND booking.id < :cursorId))
            ORDER BY booking.createdAt DESC, booking.id DESC
            """)
    List<Booking> findExpertHistory(
            @Param("expertUserId") UUID expertUserId,
            @Param("status") BookingStatus status,
            @Param("cursorCreatedAt") OffsetDateTime cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT booking
            FROM Booking booking
            WHERE booking.status = com.mindcare.bookingservice.booking.entity.BookingStatus.CANCELLATION_PENDING
              AND booking.cancellationReviewDeadline <= :now
              AND booking.deletedAt IS NULL
            ORDER BY booking.cancellationReviewDeadline ASC
            """)
    List<Booking> findExpiredCancellationRequestsForUpdate(
            @Param("now") OffsetDateTime now,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT booking
            FROM Booking booking
            WHERE booking.status = com.mindcare.bookingservice.booking.entity.BookingStatus.PAYMENT_PENDING
              AND booking.expiresAt <= :now
              AND booking.deletedAt IS NULL
            ORDER BY booking.expiresAt ASC
            """)
    List<Booking> findExpiredPaymentBookingsForUpdate(
            @Param("now") OffsetDateTime now,
            Pageable pageable);

    long countByExpertUserIdAndStatusAndDeletedAtIsNull(UUID expertUserId, BookingStatus status);
}
