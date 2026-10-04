package com.mindcare.emotionservice.journal.dto;

import com.mindcare.emotionservice.journal.entity.EmotionType;
import com.mindcare.emotionservice.journal.entity.JournalEntrySource;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EmotionJournalResponse(
        UUID id,
        EmotionType emotionType,
        String content,
        Integer energyLevel,
        Integer stressLevel,
        Integer sleepQuality,
        JournalEntrySource source,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public EmotionJournalResponse(UUID id, EmotionType emotionType, String content,
                                  Integer energyLevel, Integer stressLevel, Integer sleepQuality,
                                  OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this(id, emotionType, content, energyLevel, stressLevel, sleepQuality,
                JournalEntrySource.USER_DIRECT, createdAt, updatedAt);
    }

    public EmotionJournalResponse(UUID id, EmotionType emotionType, String content,
                                  OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this(id, emotionType, content, null, null, null, JournalEntrySource.USER_DIRECT, createdAt, updatedAt);
    }
}
