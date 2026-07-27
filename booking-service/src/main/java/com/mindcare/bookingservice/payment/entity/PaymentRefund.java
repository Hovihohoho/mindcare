package com.mindcare.bookingservice.payment.entity;

import com.mindcare.bookingservice.shared.entity.AuditableEntity;
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

@Getter
@Entity
@Table(name = "payment_refunds", schema = "booking_schema")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentRefund extends AuditableEntity {

    @Column(name = "payment_id", nullable = false, updatable = false)
    private UUID paymentId;

    @Column(name = "booking_id", nullable = false, updatable = false)
    private UUID bookingId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3, columnDefinition = "CHAR(3)")
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RefundReason reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RefundStatus status;

    @Column(name = "provider_refund_id")
    private String providerRefundId;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "requested_at", nullable = false)
    private OffsetDateTime requestedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Version
    @Column(nullable = false)
    private long version;

    public static PaymentRefund pending(
            Payment payment,
            RefundReason reason,
            OffsetDateTime requestedAt) {
        PaymentRefund refund = new PaymentRefund();
        refund.paymentId = payment.getId();
        refund.bookingId = payment.getBookingId();
        refund.amount = payment.getAmount();
        refund.currency = payment.getCurrency();
        refund.reason = reason;
        refund.status = RefundStatus.PENDING;
        refund.requestedAt = requestedAt;
        return refund;
    }

    public void succeed(String providerRefundId, OffsetDateTime now) {
        if (status != RefundStatus.PENDING && status != RefundStatus.FAILED) {
            return;
        }
        status = RefundStatus.SUCCEEDED;
        this.providerRefundId = providerRefundId;
        completedAt = now;
        failureCode = null;
    }

    public void fail(String failureCode) {
        if (status == RefundStatus.SUCCEEDED) {
            return;
        }
        status = RefundStatus.FAILED;
        this.failureCode = failureCode;
    }

    public void retry(OffsetDateTime now) {
        status = RefundStatus.PENDING;
        requestedAt = now;
        failureCode = null;
    }
}
