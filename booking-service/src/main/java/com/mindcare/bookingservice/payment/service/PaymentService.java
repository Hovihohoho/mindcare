package com.mindcare.bookingservice.payment.service;

import com.mindcare.bookingservice.payment.dto.PaymentResponse;
import com.mindcare.bookingservice.payment.dto.RefundResponse;
import com.mindcare.bookingservice.payment.dto.VerifiedPaymentCallback;
import com.mindcare.bookingservice.payment.entity.PaymentMethod;
import com.mindcare.bookingservice.payment.entity.RefundReason;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PaymentService {

    PaymentResponse createCheckout(
            PaymentCheckoutContext context,
            PaymentMethod method,
            String idempotencyKey,
            OffsetDateTime expiresAt);

    Optional<PaymentResponse> findByBooking(UUID bookingId, UUID userId);

    void processVerifiedCallback(VerifiedPaymentCallback callback);

    RefundResponse requestFullRefund(UUID bookingId, RefundReason reason);

    RefundResponse processRefund(UUID refundId);
}
