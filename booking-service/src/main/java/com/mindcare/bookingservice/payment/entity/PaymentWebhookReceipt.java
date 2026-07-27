package com.mindcare.bookingservice.payment.entity;

import com.mindcare.bookingservice.shared.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "payment_webhook_receipts", schema = "booking_schema")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentWebhookReceipt extends AuditableEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentMethod provider;

    @Column(name = "provider_event_id", nullable = false)
    private String providerEventId;

    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @Column(name = "signature_verified", nullable = false)
    private boolean signatureVerified;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    private WebhookProcessingStatus processingStatus;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    public static PaymentWebhookReceipt received(
            PaymentMethod provider,
            String providerEventId,
            String payloadHash,
            boolean signatureVerified) {
        PaymentWebhookReceipt receipt = new PaymentWebhookReceipt();
        receipt.provider = provider;
        receipt.providerEventId = providerEventId;
        receipt.payloadHash = payloadHash;
        receipt.signatureVerified = signatureVerified;
        receipt.processingStatus = signatureVerified
                ? WebhookProcessingStatus.RECEIVED
                : WebhookProcessingStatus.REJECTED;
        return receipt;
    }

    public void processed(OffsetDateTime now) {
        processingStatus = WebhookProcessingStatus.PROCESSED;
        processedAt = now;
    }

    public void rejected(OffsetDateTime now) {
        processingStatus = WebhookProcessingStatus.REJECTED;
        processedAt = now;
    }
}
