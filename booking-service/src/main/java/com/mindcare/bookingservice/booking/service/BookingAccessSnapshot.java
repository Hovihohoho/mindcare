package com.mindcare.bookingservice.booking.service;

import com.mindcare.bookingservice.booking.entity.BookingPaymentStatus;
import com.mindcare.bookingservice.booking.entity.BookingStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BookingAccessSnapshot(
        UUID bookingId,
        UUID userId,
        UUID expertUserId,
        UUID scheduleId,
        BookingStatus status,
        BookingPaymentStatus paymentStatus,
        OffsetDateTime startAt,
        OffsetDateTime endAt) {

    public boolean isParticipant(UUID actorId) {
        return userId.equals(actorId) || expertUserId.equals(actorId);
    }
}
