package com.mindcare.emotionservice.journal.dto;

import com.mindcare.emotionservice.journal.entity.EmotionType;
import com.mindcare.emotionservice.journal.entity.JournalEntrySource;
import com.mindcare.emotionservice.shared.validation.MaxCodePoints;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CreateEmotionJournalRequest(
        @NotNull
        EmotionType emotionType,
        @MaxCodePoints(5_000)
        String content,
        @Min(1) @Max(5) Integer energyLevel,
        @Min(1) @Max(5) Integer stressLevel,
        @Min(1) @Max(5) Integer sleepQuality,
        JournalEntrySource source
) {
    public CreateEmotionJournalRequest(EmotionType emotionType, String content) {
        this(emotionType, content, null, null, null, null);
    }

    public CreateEmotionJournalRequest(EmotionType emotionType, String content,
                                       Integer energyLevel, Integer stressLevel, Integer sleepQuality) {
        this(emotionType, content, energyLevel, stressLevel, sleepQuality, null);
    }
}
