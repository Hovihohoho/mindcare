package com.mindcare.ai_service.repository;

import com.mindcare.ai_service.entity.AiConversation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiConversationRepository extends JpaRepository<AiConversation, UUID> {
    List<AiConversation> findTop50ByUserIdOrderByUpdatedAtDesc(UUID userId);
    List<AiConversation> findByUserIdOrderByUpdatedAtDesc(UUID userId);
    Optional<AiConversation> findByIdAndUserId(UUID id, UUID userId);
    long deleteByUpdatedAtBefore(Instant cutoff);
    long deleteByUserId(UUID userId);
}
