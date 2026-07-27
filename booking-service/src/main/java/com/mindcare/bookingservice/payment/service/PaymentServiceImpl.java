package com.mindcare.bookingservice.payment.service;

import com.mindcare.bookingservice.booking.service.BookingPaymentLifecycleService;
import com.mindcare.bookingservice.integration.outbox.service.OutboxService;
import com.mindcare.bookingservice.payment.dto.PaymentResponse;
import com.mindcare.bookingservice.payment.dto.RefundResponse;
import com.mindcare.bookingservice.payment.dto.VerifiedPaymentCallback;
import com.mindcare.bookingservice.payment.entity.Payment;
import com.mindcare.bookingservice.payment.entity.PaymentMethod;
import com.mindcare.bookingservice.payment.entity.PaymentRefund;
import com.mindcare.bookingservice.payment.entity.PaymentStatus;
import com.mindcare.bookingservice.payment.entity.PaymentWebhookReceipt;
import com.mindcare.bookingservice.payment.entity.RefundReason;
import com.mindcare.bookingservice.payment.integration.PaymentCheckoutCommand;
import com.mindcare.bookingservice.payment.integration.PaymentCheckoutResult;
import com.mindcare.bookingservice.payment.integration.PaymentProviderGateway;
import com.mindcare.bookingservice.payment.integration.PaymentRefundCommand;
import com.mindcare.bookingservice.payment.mapper.PaymentMapper;
import com.mindcare.bookingservice.payment.repository.PaymentRefundRepository;
import com.mindcare.bookingservice.payment.repository.PaymentRepository;
import com.mindcare.bookingservice.payment.repository.PaymentWebhookReceiptRepository;
import com.mindcare.bookingservice.shared.exception.BusinessException;
import com.mindcare.bookingservice.shared.exception.ResourceNotFoundException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentRefundRepository refundRepository;
    private final PaymentWebhookReceiptRepository receiptRepository;
    private final PaymentProviderGateway paymentProviderGateway;
    private final BookingPaymentLifecycleService bookingPaymentLifecycleService;
    private final OutboxService outboxService;
    private final PaymentMapper paymentMapper;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    @Override
    public PaymentResponse createCheckout(
            PaymentCheckoutContext context,
            PaymentMethod method,
            String idempotencyKey,
            OffsetDateTime expiresAt) {
        Optional<Payment> existing = paymentRepository
                .findByUserIdAndBookingIdAndIdempotencyKeyAndDeletedAtIsNull(
                        context.userId(),
                        context.bookingId(),
                        idempotencyKey);
        if (existing.isPresent()) {
            Payment payment = existing.get();
            if (payment.getPaymentMethod() != method
                    || payment.getAmount().compareTo(context.amount()) != 0
                    || !payment.getCurrency().equals(context.currency())) {
                throw new BusinessException(
                        "PAYMENT_IDEMPOTENCY_CONFLICT",
                        HttpStatus.CONFLICT,
                        "Idempotency key was used with a different payment request");
            }
            return paymentMapper.toResponse(payment);
        }

        String providerOrderId = UUID.randomUUID().toString();
        PaymentCheckoutResult checkout = paymentProviderGateway.createCheckout(
                new PaymentCheckoutCommand(
                        context.bookingId(),
                        context.userId(),
                        providerOrderId,
                        context.amount(),
                        context.currency(),
                        method,
                        expiresAt));

        Payment payment = Payment.pending(
                context.bookingId(),
                context.userId(),
                context.amount(),
                context.currency(),
                method,
                providerOrderId,
                idempotencyKey,
                checkout.checkoutUrl(),
                expiresAt);
        Payment saved = transactionTemplate.execute(status -> paymentRepository.save(payment));
        return paymentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PaymentResponse> findByBooking(UUID bookingId, UUID userId) {
        return paymentRepository
                .findByBookingIdAndUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                        bookingId,
                        userId)
                .stream()
                .findFirst()
                .map(paymentMapper::toResponse);
    }

    @Override
    @Transactional
    public void processVerifiedCallback(VerifiedPaymentCallback callback) {
        if (receiptRepository.findByProviderAndProviderEventIdAndDeletedAtIsNull(
                callback.provider(),
                callback.providerEventId()).isPresent()) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        PaymentWebhookReceipt receipt = receiptRepository.save(
                PaymentWebhookReceipt.received(
                        callback.provider(),
                        callback.providerEventId(),
                        callback.payloadHash(),
                        callback.signatureVerified()));
        if (!callback.signatureVerified()) {
            receipt.rejected(now);
            return;
        }

        Payment payment = paymentRepository
                .findByProviderOrderIdForUpdate(callback.providerOrderId())
                .orElse(null);
        if (payment == null) {
            receipt.rejected(now);
            return;
        }
        if (!matchesExpectedPayment(payment, callback)) {
            receipt.rejected(now);
            return;
        }

        if (!callback.succeeded()) {
            if (payment.getStatus() == PaymentStatus.PENDING) {
                payment.fail(callback.failureCode(), now);
                bookingPaymentLifecycleService.failPayment(payment.getBookingId());
                outboxService.append(
                        "PAYMENT",
                        payment.getId(),
                        "payment.failed",
                        Map.of(
                                "paymentId", payment.getId(),
                                "bookingId", payment.getBookingId(),
                                "provider", payment.getPaymentMethod().name()));
            }
            receipt.processed(now);
            return;
        }

        if (payment.getStatus() == PaymentStatus.SUCCESS
                || payment.getStatus() == PaymentStatus.REFUNDED) {
            receipt.processed(now);
            return;
        }

        boolean confirmed = bookingPaymentLifecycleService.confirmPaymentIfActive(
                payment.getBookingId(),
                now);
        payment.succeed(callback.transactionCode(), now, !confirmed);
        if (!confirmed) {
            createRefundIfAbsent(payment, RefundReason.LATE_PAYMENT_SUCCESS, now);
        }
        outboxService.append(
                "PAYMENT",
                payment.getId(),
                "payment.succeeded",
                Map.of(
                        "paymentId", payment.getId(),
                        "bookingId", payment.getBookingId(),
                        "provider", payment.getPaymentMethod().name(),
                        "lateSuccess", !confirmed));
        receipt.processed(now);
    }

    @Override
    @Transactional
    public RefundResponse requestFullRefund(UUID bookingId, RefundReason reason) {
        Payment payment = paymentRepository
                .findFirstByBookingIdAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                        bookingId,
                        PaymentStatus.SUCCESS)
                .orElseThrow(() -> new BusinessException(
                        "SUCCESSFUL_PAYMENT_NOT_FOUND",
                        HttpStatus.CONFLICT,
                        "No successful payment is available for refund"));
        PaymentRefund refund = createRefundIfAbsent(
                payment,
                reason,
                OffsetDateTime.now(clock));
        return paymentMapper.toResponse(refund);
    }

    @Override
    public RefundResponse processRefund(UUID refundId) {
        PaymentRefund refund = refundRepository.findById(refundId)
                .filter(candidate -> !candidate.isDeleted())
                .orElseThrow(ResourceNotFoundException::new);
        if (refund.getStatus()
                == com.mindcare.bookingservice.payment.entity.RefundStatus.SUCCEEDED) {
            return paymentMapper.toResponse(refund);
        }
        Payment payment = paymentRepository.findById(refund.getPaymentId())
                .filter(candidate -> !candidate.isDeleted())
                .orElseThrow(ResourceNotFoundException::new);

        String providerRefundId;
        try {
            providerRefundId = paymentProviderGateway.refund(
                    new PaymentRefundCommand(
                            refund.getId(),
                            payment.getProviderOrderId(),
                            refund.getAmount(),
                            refund.getCurrency()));
        } catch (RuntimeException providerFailure) {
            transactionTemplate.executeWithoutResult(status -> {
                PaymentRefund locked = refundRepository.findById(refundId)
                        .orElseThrow(ResourceNotFoundException::new);
                locked.fail("PROVIDER_REFUND_FAILED");
            });
            throw providerFailure;
        }

        PaymentRefund completed = transactionTemplate.execute(status -> {
            PaymentRefund lockedRefund = refundRepository.findById(refundId)
                    .orElseThrow(ResourceNotFoundException::new);
            Payment lockedPayment = paymentRepository.findById(lockedRefund.getPaymentId())
                    .orElseThrow(ResourceNotFoundException::new);
            lockedRefund.succeed(providerRefundId, OffsetDateTime.now(clock));
            lockedPayment.markRefunded();
            bookingPaymentLifecycleService.markRefunded(lockedPayment.getBookingId());
            outboxService.append(
                    "PAYMENT",
                    lockedPayment.getId(),
                    "payment.refunded",
                    Map.of(
                            "paymentId", lockedPayment.getId(),
                            "bookingId", lockedPayment.getBookingId(),
                            "refundId", lockedRefund.getId()));
            return lockedRefund;
        });
        return paymentMapper.toResponse(completed);
    }

    private PaymentRefund createRefundIfAbsent(
            Payment payment,
            RefundReason reason,
            OffsetDateTime now) {
        return refundRepository.findByPaymentIdAndDeletedAtIsNull(payment.getId())
                .orElseGet(() -> refundRepository.save(PaymentRefund.pending(payment, reason, now)));
    }

    private boolean matchesExpectedPayment(
            Payment payment,
            VerifiedPaymentCallback callback) {
        return payment.getPaymentMethod() == callback.provider()
                && payment.getAmount().compareTo(callback.amount()) == 0
                && payment.getCurrency().equals(callback.currency());
    }
}
