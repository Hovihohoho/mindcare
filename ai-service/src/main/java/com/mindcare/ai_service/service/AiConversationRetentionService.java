package com.mindcare.ai_service.service;

import com.mindcare.ai_service.repository.AiConversationRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiConversationRetentionService {
    private final AiConversationRepository conversationRepository;

    @Value("${ai.conversation-retention-days:365}")
    private long retentionDays;

    @Scheduled(cron = "${ai.conversation-retention-cron:0 20 3 * * *}")
    @Transactional
    public long deleteExpiredConversations() {
        if (retentionDays < 1) {
            throw new IllegalStateException("AI conversation retention must be at least one day.");
        }
        return conversationRepository.deleteByUpdatedAtBefore(
                Instant.now().minus(retentionDays, ChronoUnit.DAYS));
    }
}
