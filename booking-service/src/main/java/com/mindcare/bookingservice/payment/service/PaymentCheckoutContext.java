package com.mindcare.bookingservice.payment.service;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentCheckoutContext(
        UUID bookingId,
        UUID userId,
        BigDecimal amount,
        String currency) {
}
