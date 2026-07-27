package com.mindcare.emotionservice.journal.service;

import com.mindcare.emotionservice.journal.dto.CreateEmotionJournalRequest;
import com.mindcare.emotionservice.journal.dto.EmotionJournalResponse;
import com.mindcare.emotionservice.journal.dto.EmotionTrendPointResponse;
import com.mindcare.emotionservice.shared.dto.CursorPageResponse;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

public interface EmotionJournalService {

    EmotionJournalResponse createJournal(UUID userId, CreateEmotionJournalRequest request);

    EmotionJournalResponse getJournal(UUID userId, UUID journalId);

    CursorPageResponse<EmotionJournalResponse> getJournalHistory(
            UUID userId,
            OffsetDateTime from,
            OffsetDateTime to,
            String cursor,
            int limit
    );

    void deleteJournal(UUID userId, UUID journalId);

    List<EmotionTrendPointResponse> getEmotionTrends(
            UUID userId,
            OffsetDateTime from,
            OffsetDateTime to,
            String bucket,
            ZoneId timezone
    );
}
