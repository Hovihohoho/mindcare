package com.mindcare.bookingservice.chat.entity;

import com.mindcare.bookingservice.shared.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "conversations", schema = "booking_schema")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Conversation extends AuditableEntity {

    @Column(name = "booking_id", updatable = false)
    private UUID bookingId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "expert_user_id", nullable = false, updatable = false)
    private UUID expertUserId;

    @Column(name = "opened_at", nullable = false)
    private OffsetDateTime openedAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    public static Conversation create(UUID userId, UUID expertUserId, OffsetDateTime openedAt) {
        Conversation conversation = new Conversation();
        conversation.userId = userId;
        conversation.expertUserId = expertUserId;
        conversation.openedAt = openedAt;
        return conversation;
    }

    public boolean isParticipant(UUID actorId) {
        return userId.equals(actorId) || expertUserId.equals(actorId);
    }

    public void close(OffsetDateTime now) {
        closedAt = now;
    }
}
