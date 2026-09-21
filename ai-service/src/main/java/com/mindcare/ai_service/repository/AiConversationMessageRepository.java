package com.mindcare.ai_service.repository;

import com.mindcare.ai_service.entity.AiConversationMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiConversationMessageRepository extends JpaRepository<AiConversationMessage, UUID> {
    List<AiConversationMessage> findByConversationIdOrderByCreatedAtAscIdAsc(UUID conversationId);
    List<AiConversationMessage> findTop6ByConversationIdOrderByCreatedAtDescIdDesc(UUID conversationId);
}
