package com.mindcare.emotionservice.journal.dto;

import com.mindcare.emotionservice.journal.entity.EmotionType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EmotionJournalResponse(
        UUID id,
        EmotionType emotionType,
        String content,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
