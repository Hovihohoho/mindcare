package com.mindcare.bookingservice.chat.dto;

import com.mindcare.bookingservice.booking.entity.BookingStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ConversationHistoryResponse(
        UUID id,
        UUID bookingId,
        UUID userId,
        UUID expertUserId,
        BookingStatus bookingStatus,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        OffsetDateTime openedAt) {
}
