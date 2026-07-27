package com.mindcare.bookingservice.payment.dto;

import com.mindcare.bookingservice.payment.entity.PaymentMethod;
import com.mindcare.bookingservice.payment.entity.PaymentStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID bookingId,
        BigDecimal amount,
        String currency,
        PaymentMethod paymentMethod,
        String providerOrderId,
        PaymentStatus status,
        String checkoutUrl,
        OffsetDateTime expiresAt,
        OffsetDateTime paidAt,
        boolean lateSuccess,
        OffsetDateTime createdAt) {
}
