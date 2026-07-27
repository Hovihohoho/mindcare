package com.mindcare.bookingservice.booking.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mindcare.bookingservice.shared.exception.BusinessException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BookingTest {

    @Test
    void instantBookingConfirmsOnlyAfterPayment() {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-01T08:00:00Z");
        Booking booking = pending(now);

        booking.confirmPayment(now.plusMinutes(1));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getPaymentStatus()).isEqualTo(BookingPaymentStatus.PAID);
        assertThat(booking.getConfirmedAt()).isEqualTo(now.plusMinutes(1));
        assertThat(booking.getExpiresAt()).isNull();
    }

    @Test
    void pendingCancellationCanTimeoutBackToConfirmed() {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-01T08:00:00Z");
        Booking booking = pending(now);
        booking.confirmPayment(now.plusMinutes(1));
        booking.requestCancellation("schedule conflict", now.plusHours(1), now.plusHours(2));

        booking.timeoutCancellation(now.plusHours(2));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getCancellationDecisionReason())
                .isEqualTo("CANCELLATION_REQUEST_EXPIRED");
    }

    @Test
    void unconfirmedBookingCannotBeCompleted() {
        Booking booking = pending(OffsetDateTime.parse("2026-08-01T08:00:00Z"));

        assertThatThrownBy(() ->
                booking.complete(OffsetDateTime.parse("2026-08-01T10:00:00Z")))
                .isInstanceOf(BusinessException.class);
    }

    private Booking pending(OffsetDateTime now) {
        return Booking.createPaymentPending(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "idem-1",
                "note",
                BigDecimal.valueOf(300000),
                "VND",
                now.plusMinutes(15));
    }
}
