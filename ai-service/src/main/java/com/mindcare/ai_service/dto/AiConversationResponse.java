package com.mindcare.ai_service.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AiConversationResponse(
        UUID id,
        String title,
        Instant createdAt,
        Instant updatedAt,
        List<Message> messages
) {
    public record Message(
            UUID id,
            String role,
            String content,
            List<RagChatResponse.Source> sources,
            String safetyLevel,
            Instant createdAt
    ) {}
}
