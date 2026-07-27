package com.mindcare.bookingservice.payment.entity;

import com.mindcare.bookingservice.shared.entity.AuditableEntity;
import com.mindcare.bookingservice.shared.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
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
@Table(name = "payments", schema = "booking_schema")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends AuditableEntity {

    @Column(name = "booking_id", nullable = false, updatable = false)
    private UUID bookingId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3, columnDefinition = "CHAR(3)")
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "provider_order_id", nullable = false)
    private String providerOrderId;

    @Column(name = "transaction_code")
    private String transactionCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "checkout_url", length = 2000)
    private String checkoutUrl;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "failed_at")
    private OffsetDateTime failedAt;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "late_success", nullable = false)
    private boolean lateSuccess;

    public static Payment pending(
            UUID bookingId,
            UUID userId,
            BigDecimal amount,
            String currency,
            PaymentMethod method,
            String providerOrderId,
            String idempotencyKey,
            String checkoutUrl,
            OffsetDateTime expiresAt) {
        Payment payment = new Payment();
        payment.bookingId = bookingId;
        payment.userId = userId;
        payment.amount = amount;
        payment.currency = currency;
        payment.paymentMethod = method;
        payment.providerOrderId = providerOrderId;
        payment.idempotencyKey = idempotencyKey;
        payment.checkoutUrl = checkoutUrl;
        payment.expiresAt = expiresAt;
        payment.status = PaymentStatus.PENDING;
        payment.lateSuccess = false;
        return payment;
    }

    public void succeed(String transactionCode, OffsetDateTime now, boolean lateSuccess) {
        if (status != PaymentStatus.PENDING
                && status != PaymentStatus.EXPIRED
                && status != PaymentStatus.FAILED) {
            throw invalidTransition();
        }
        status = PaymentStatus.SUCCESS;
        this.transactionCode = transactionCode;
        paidAt = now;
        expiresAt = null;
        this.lateSuccess = lateSuccess;
    }

    public void fail(String failureCode, OffsetDateTime now) {
        requirePending();
        status = PaymentStatus.FAILED;
        this.failureCode = failureCode;
        failedAt = now;
        expiresAt = null;
    }

    public void expire() {
        requirePending();
        status = PaymentStatus.EXPIRED;
        expiresAt = null;
    }

    public void markRefunded() {
        if (status != PaymentStatus.SUCCESS) {
            throw invalidTransition();
        }
        status = PaymentStatus.REFUNDED;
    }

    private void requirePending() {
        if (status != PaymentStatus.PENDING) {
            throw invalidTransition();
        }
    }

    private BusinessException invalidTransition() {
        return new BusinessException(
                "INVALID_PAYMENT_TRANSITION",
                HttpStatus.CONFLICT,
                "Payment state transition is not allowed");
    }
}
