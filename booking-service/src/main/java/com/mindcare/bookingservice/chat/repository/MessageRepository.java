package com.mindcare.bookingservice.chat.repository;

import com.mindcare.bookingservice.chat.entity.Message;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    @Query("""
            SELECT message
            FROM Message message
            WHERE message.conversationId = :conversationId
              AND message.deletedAt IS NULL
              AND (:cursorCreatedAt IS NULL
                    OR message.createdAt < :cursorCreatedAt
                    OR (message.createdAt = :cursorCreatedAt AND message.id < :cursorId))
            ORDER BY message.createdAt DESC, message.id DESC
            """)
    List<Message> findHistory(
            @Param("conversationId") UUID conversationId,
            @Param("cursorCreatedAt") OffsetDateTime cursorCreatedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable);

    @Modifying
    @Query("""
            UPDATE Message message
            SET message.read = TRUE,
                message.readAt = :readAt
            WHERE message.conversationId = :conversationId
              AND message.senderId <> :readerId
              AND message.read = FALSE
              AND message.deletedAt IS NULL
            """)
    int markConversationRead(
            @Param("conversationId") UUID conversationId,
            @Param("readerId") UUID readerId,
            @Param("readAt") OffsetDateTime readAt);
}
