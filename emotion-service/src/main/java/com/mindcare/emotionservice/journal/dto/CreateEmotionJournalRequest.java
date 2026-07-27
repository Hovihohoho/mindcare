package com.mindcare.emotionservice.journal.dto;

import com.mindcare.emotionservice.journal.entity.EmotionType;
import com.mindcare.emotionservice.shared.validation.MaxCodePoints;
import jakarta.validation.constraints.NotNull;

public record CreateEmotionJournalRequest(
        @NotNull
        EmotionType emotionType,
        @MaxCodePoints(5_000)
        String content
) {
}
