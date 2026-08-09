package com.mindcare.bookingservice.chat.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConversationHistoryResponse(
        UUID id,
        UUID userId,
        UUID expertUserId,
        OffsetDateTime openedAt,
        OffsetDateTime closedAt,
        boolean writable) {
}
