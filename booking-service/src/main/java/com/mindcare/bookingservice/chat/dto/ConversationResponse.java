package com.mindcare.bookingservice.chat.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConversationResponse(
        UUID id,
        UUID bookingId,
        OffsetDateTime openedAt,
        OffsetDateTime closedAt,
        boolean writable) {
}
