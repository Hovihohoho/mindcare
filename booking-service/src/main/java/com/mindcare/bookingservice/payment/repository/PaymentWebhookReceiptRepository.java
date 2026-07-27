package com.mindcare.bookingservice.payment.repository;

import com.mindcare.bookingservice.payment.entity.PaymentMethod;
import com.mindcare.bookingservice.payment.entity.PaymentWebhookReceipt;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentWebhookReceiptRepository
        extends JpaRepository<PaymentWebhookReceipt, UUID> {

    Optional<PaymentWebhookReceipt> findByProviderAndProviderEventIdAndDeletedAtIsNull(
            PaymentMethod provider,
            String providerEventId);
}
