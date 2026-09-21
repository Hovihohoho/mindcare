package com.mindcare.ai_service.repository;

import com.mindcare.ai_service.entity.AiConversation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiConversationRepository extends JpaRepository<AiConversation, UUID> {
    org.springframework.data.domain.Slice<AiConversation> findByUserIdOrderByIdAsc(
            UUID userId, org.springframework.data.domain.Pageable pageable);
    List<AiConversation> findByUserIdOrderByUpdatedAtDesc(UUID userId);
    Optional<AiConversation> findByIdAndUserId(UUID id, UUID userId);
    long deleteByUpdatedAtBefore(Instant cutoff);
    long deleteByUserId(UUID userId);
}
