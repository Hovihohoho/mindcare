package com.mindcare.bookingservice.chat.entity;

import com.mindcare.bookingservice.shared.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "messages", schema = "booking_schema")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Message extends AuditableEntity {

    @Column(name = "conversation_id", nullable = false, updatable = false)
    private UUID conversationId;

    @Column(name = "sender_id", nullable = false, updatable = false)
    private UUID senderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private MessageType messageType;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "attachment_url", length = 1000)
    private String attachmentUrl;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    public static Message text(UUID conversationId, UUID senderId, String content) {
        Message message = new Message();
        message.conversationId = conversationId;
        message.senderId = senderId;
        message.messageType = MessageType.TEXT;
        message.content = content.trim();
        message.read = false;
        return message;
    }

    public void markRead(OffsetDateTime now) {
        if (!read) {
            read = true;
            readAt = now;
        }
    }
}
