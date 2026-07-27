package com.mindcare.bookingservice.chat.dto;

import com.mindcare.bookingservice.chat.entity.MessageType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        UUID senderId,
        MessageType messageType,
        String content,
        boolean read,
        OffsetDateTime readAt,
        OffsetDateTime createdAt) {
}
