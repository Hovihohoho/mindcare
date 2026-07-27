package com.mindcare.bookingservice.payment.integration;

import com.mindcare.bookingservice.payment.entity.PaymentMethod;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentCheckoutCommand(
        UUID bookingId,
        UUID userId,
        String providerOrderId,
        BigDecimal amount,
        String currency,
        PaymentMethod method,
        OffsetDateTime expiresAt) {
}
