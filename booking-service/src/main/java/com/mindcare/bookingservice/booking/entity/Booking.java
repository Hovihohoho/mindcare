package com.mindcare.bookingservice.booking.entity;

import com.mindcare.bookingservice.shared.entity.AuditableEntity;
import com.mindcare.bookingservice.shared.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.http.HttpStatus;

@Getter
@Entity
@Table(name = "bookings", schema = "booking_schema")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Booking extends AuditableEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "expert_user_id", nullable = false, updatable = false)
    private UUID expertUserId;

    @Column(name = "schedule_id", nullable = false, updatable = false)
    private UUID scheduleId;

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3, columnDefinition = "CHAR(3)")
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private BookingPaymentStatus paymentStatus;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "canceled_by", length = 20)
    private CancellationActor canceledBy;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;

    @Column(name = "cancellation_requested_at")
    private OffsetDateTime cancellationRequestedAt;

    @Column(name = "cancellation_review_deadline")
    private OffsetDateTime cancellationReviewDeadline;

    @Column(name = "cancellation_decided_at")
    private OffsetDateTime cancellationDecidedAt;

    @Column(name = "cancellation_decision_reason", columnDefinition = "TEXT")
    private String cancellationDecisionReason;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "canceled_at")
    private OffsetDateTime canceledAt;

    @Version
    @Column(nullable = false)
    private long version;

    public static Booking createPaymentPending(
            UUID userId,
            UUID expertUserId,
            UUID scheduleId,
            String idempotencyKey,
            String note,
            BigDecimal price,
            String currency,
            OffsetDateTime expiresAt) {
        Booking booking = new Booking();
        booking.userId = userId;
        booking.expertUserId = expertUserId;
        booking.scheduleId = scheduleId;
        booking.idempotencyKey = idempotencyKey;
        booking.note = normalize(note);
        booking.price = price;
        booking.currency = currency;
        booking.status = BookingStatus.PAYMENT_PENDING;
        booking.paymentStatus = BookingPaymentStatus.UNPAID;
        booking.expiresAt = expiresAt;
        return booking;
    }

    public void confirmPayment(OffsetDateTime now) {
        requireStatus(BookingStatus.PAYMENT_PENDING);
        status = BookingStatus.CONFIRMED;
        paymentStatus = BookingPaymentStatus.PAID;
        confirmedAt = now;
        expiresAt = null;
    }

    public void confirmWithoutPayment(OffsetDateTime now) {
        requireStatus(BookingStatus.PAYMENT_PENDING);
        status = BookingStatus.CONFIRMED;
        confirmedAt = now;
        expiresAt = null;
    }

    public void markPaymentFailed() {
        requireStatus(BookingStatus.PAYMENT_PENDING);
        status = BookingStatus.PAYMENT_FAILED;
        expiresAt = null;
    }

    public void expire() {
        requireStatus(BookingStatus.PAYMENT_PENDING);
        status = BookingStatus.EXPIRED;
        expiresAt = null;
    }

    public void cancelImmediately(
            CancellationActor actor,
            String reason,
            OffsetDateTime now) {
        requireStatus(BookingStatus.CONFIRMED);
        status = BookingStatus.CANCELED;
        canceledBy = actor;
        cancellationReason = normalizeRequired(reason);
        canceledAt = now;
    }

    public void requestCancellation(
            String reason,
            OffsetDateTime now,
            OffsetDateTime reviewDeadline) {
        requireStatus(BookingStatus.CONFIRMED);
        status = BookingStatus.CANCELLATION_PENDING;
        cancellationReason = normalizeRequired(reason);
        cancellationRequestedAt = now;
        cancellationReviewDeadline = reviewDeadline;
    }

    public void approveCancellation(
            String decisionReason,
            OffsetDateTime now) {
        requireStatus(BookingStatus.CANCELLATION_PENDING);
        status = BookingStatus.CANCELED;
        canceledBy = CancellationActor.USER;
        cancellationDecisionReason = normalize(decisionReason);
        cancellationDecidedAt = now;
        canceledAt = now;
    }

    public void cancelByExpert(String reason, OffsetDateTime now) {
        if (status != BookingStatus.CONFIRMED
                && status != BookingStatus.CANCELLATION_PENDING) {
            throw invalidTransition("Booking cannot be canceled by expert");
        }
        status = BookingStatus.CANCELED;
        canceledBy = CancellationActor.EXPERT;
        cancellationReason = normalizeRequired(reason);
        cancellationDecisionReason = "EXPERT_CANCELED";
        cancellationDecidedAt = now;
        canceledAt = now;
    }

    public void rejectCancellation(
            String decisionReason,
            OffsetDateTime now) {
        requireStatus(BookingStatus.CANCELLATION_PENDING);
        status = BookingStatus.CONFIRMED;
        cancellationDecisionReason = normalizeRequired(decisionReason);
        cancellationDecidedAt = now;
    }

    public void timeoutCancellation(OffsetDateTime now) {
        requireStatus(BookingStatus.CANCELLATION_PENDING);
        status = BookingStatus.CONFIRMED;
        cancellationDecisionReason = "CANCELLATION_REQUEST_EXPIRED";
        cancellationDecidedAt = now;
    }

    public void complete(OffsetDateTime now) {
        requireStatus(BookingStatus.CONFIRMED);
        status = BookingStatus.COMPLETED;
        completedAt = now;
    }

    public void markUserNoShow(OffsetDateTime now) {
        requireStatus(BookingStatus.CONFIRMED);
        status = BookingStatus.USER_NO_SHOW;
        completedAt = now;
    }

    public void markExpertNoShow(OffsetDateTime now) {
        requireStatus(BookingStatus.CONFIRMED);
        status = BookingStatus.EXPERT_NO_SHOW;
        completedAt = now;
    }

    public void markRefunded() {
        if (status != BookingStatus.CANCELED
                && status != BookingStatus.EXPERT_NO_SHOW
                && status != BookingStatus.EXPIRED
                && status != BookingStatus.PAYMENT_FAILED) {
            throw invalidTransition("Booking is not eligible for refund completion");
        }
        paymentStatus = BookingPaymentStatus.REFUNDED;
    }

    private void requireStatus(BookingStatus expected) {
        if (status != expected) {
            throw invalidTransition("Booking is not " + expected);
        }
    }

    private BusinessException invalidTransition(String message) {
        return new BusinessException(
                "INVALID_BOOKING_TRANSITION",
                HttpStatus.CONFLICT,
                message);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String normalizeRequired(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            throw new BusinessException(
                    "CANCELLATION_REASON_REQUIRED",
                    HttpStatus.BAD_REQUEST,
                    "Cancellation reason is required");
        }
        return normalized;
    }
}
