package com.mindcare.bookingservice.booking.service;

import com.mindcare.bookingservice.booking.entity.Booking;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface BookingReservationService {

    Booking reserve(
            UUID userId,
            ScheduleSnapshotForBooking schedule,
            String idempotencyKey,
            String note,
            BigDecimal price,
            String currency,
            OffsetDateTime expiresAt);

    Booking confirmWithoutPayment(UUID bookingId, OffsetDateTime confirmedAt);

    void failCheckout(UUID bookingId);

    record ScheduleSnapshotForBooking(
            UUID scheduleId,
            UUID expertUserId,
            OffsetDateTime startAt,
            OffsetDateTime endAt) {
    }
}
