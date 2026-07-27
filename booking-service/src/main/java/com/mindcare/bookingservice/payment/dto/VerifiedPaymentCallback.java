package com.mindcare.bookingservice.payment.dto;

import com.mindcare.bookingservice.payment.entity.PaymentMethod;
import java.math.BigDecimal;

public record VerifiedPaymentCallback(
        PaymentMethod provider,
        String providerEventId,
        String providerOrderId,
        String transactionCode,
        BigDecimal amount,
        String currency,
        boolean succeeded,
        String failureCode,
        String payloadHash,
        boolean signatureVerified) {
}
