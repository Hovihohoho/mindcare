package com.mindcare.bookingservice.payment.integration;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRefundCommand(
        UUID refundId,
        String providerOrderId,
        BigDecimal amount,
        String currency) {
}
