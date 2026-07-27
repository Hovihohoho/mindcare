package com.mindcare.bookingservice.payment.repository;

import com.mindcare.bookingservice.payment.entity.Payment;
import com.mindcare.bookingservice.payment.entity.PaymentStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByUserIdAndBookingIdAndIdempotencyKeyAndDeletedAtIsNull(
            UUID userId,
            UUID bookingId,
            String idempotencyKey);

    Optional<Payment> findFirstByBookingIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID bookingId,
            PaymentStatus status);

    List<Payment> findByBookingIdAndUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID bookingId,
            UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT payment
            FROM Payment payment
            WHERE payment.providerOrderId = :providerOrderId
              AND payment.deletedAt IS NULL
            """)
    Optional<Payment> findByProviderOrderIdForUpdate(
            @Param("providerOrderId") String providerOrderId);
}
