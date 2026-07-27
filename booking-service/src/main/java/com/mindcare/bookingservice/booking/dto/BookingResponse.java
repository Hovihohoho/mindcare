package com.mindcare.bookingservice.booking.dto;

import com.mindcare.bookingservice.booking.entity.BookingPaymentStatus;
import com.mindcare.bookingservice.booking.entity.BookingStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        UUID userId,
        UUID expertUserId,
        UUID scheduleId,
        BookingStatus status,
        String note,
        BigDecimal price,
        String currency,
        BookingPaymentStatus paymentStatus,
        String cancellationReason,
        OffsetDateTime expiresAt,
        OffsetDateTime confirmedAt,
        OffsetDateTime cancellationRequestedAt,
        OffsetDateTime cancellationReviewDeadline,
        String cancellationDecisionReason,
        OffsetDateTime completedAt,
        OffsetDateTime canceledAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
