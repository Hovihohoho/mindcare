package com.mindcare.bookingservice.payment.dto;

import com.mindcare.bookingservice.payment.entity.RefundReason;
import com.mindcare.bookingservice.payment.entity.RefundStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RefundResponse(
        UUID id,
        UUID paymentId,
        UUID bookingId,
        BigDecimal amount,
        String currency,
        RefundReason reason,
        RefundStatus status,
        String failureCode,
        OffsetDateTime requestedAt,
        OffsetDateTime completedAt) {
}
