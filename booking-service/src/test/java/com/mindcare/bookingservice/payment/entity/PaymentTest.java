package com.mindcare.bookingservice.payment.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentTest {

    @Test
    void expiredPaymentCanRecordLateSuccessWithoutChangingBookingHere() {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-01T08:00:00Z");
        Payment payment = pending(now);
        payment.expire();

        payment.succeed("transaction-1", now.plusMinutes(20), true);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.isLateSuccess()).isTrue();
        assertThat(payment.getPaidAt()).isEqualTo(now.plusMinutes(20));
    }

    @Test
    void refundHasIndependentLifecycle() {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-01T08:00:00Z");
        Payment payment = pending(now);
        org.springframework.test.util.ReflectionTestUtils.setField(
                payment,
                "id",
                UUID.randomUUID());
        PaymentRefund refund =
                PaymentRefund.pending(payment, RefundReason.LATE_PAYMENT_SUCCESS, now);

        refund.fail("TEMPORARY_PROVIDER_ERROR");
        refund.retry(now.plusMinutes(5));
        refund.succeed("refund-1", now.plusMinutes(6));

        assertThat(refund.getStatus()).isEqualTo(RefundStatus.SUCCEEDED);
        assertThat(refund.getFailureCode()).isNull();
    }

    private Payment pending(OffsetDateTime now) {
        return Payment.pending(
                UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.valueOf(300000),
                "VND",
                PaymentMethod.VNPAY,
                "order-1",
                "idem-1",
                "https://payment.example/checkout",
                now.plusMinutes(15));
    }
}
