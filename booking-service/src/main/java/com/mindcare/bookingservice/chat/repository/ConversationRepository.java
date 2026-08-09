package com.mindcare.bookingservice.chat.repository;

import com.mindcare.bookingservice.chat.entity.Conversation;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import java.time.OffsetDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByUserIdAndExpertUserIdAndDeletedAtIsNull(UUID userId, UUID expertUserId);

    Optional<Conversation> findByIdAndDeletedAtIsNull(UUID id);

    @Query("""
            SELECT conversation
            FROM Conversation conversation
            WHERE conversation.deletedAt IS NULL
              AND (conversation.userId = :actorId OR conversation.expertUserId = :actorId)
            ORDER BY conversation.openedAt DESC, conversation.id DESC
            """)
    List<Conversation> findHistoryForParticipant(
            @Param("actorId") UUID actorId,
            Pageable pageable);

    @Query("""
            SELECT conversation
            FROM Conversation conversation
            WHERE conversation.deletedAt IS NULL
              AND (conversation.userId = :actorId OR conversation.expertUserId = :actorId)
              AND (conversation.openedAt < :cursorOpenedAt
                    OR (conversation.openedAt = :cursorOpenedAt
                        AND conversation.id < :cursorId))
            ORDER BY conversation.openedAt DESC, conversation.id DESC
            """)
    List<Conversation> findHistoryForParticipantAfter(
            @Param("actorId") UUID actorId,
            @Param("cursorOpenedAt") OffsetDateTime cursorOpenedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable);
}
