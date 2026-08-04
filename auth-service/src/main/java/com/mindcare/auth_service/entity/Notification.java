package com.mindcare.auth_service.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "notifications", schema = "auth_schema")
@NoArgsConstructor
public class Notification {
    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "source_event_id") private UUID sourceEventId;
    @Column(nullable = false, length = 40) private String type;
    @Column(nullable = false, length = 160) private String title;
    @Column(nullable = false, length = 500) private String message;
    @Column(name = "action_url", length = 500) private String actionUrl;
    @Column(name = "read_at") private OffsetDateTime readAt;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;

    public static Notification create(UUID userId, UUID sourceEventId, String type,
                                      String title, String message, String actionUrl) {
        Notification value = new Notification();
        value.id = UUID.randomUUID();
        value.userId = userId;
        value.sourceEventId = sourceEventId;
        value.type = type;
        value.title = title;
        value.message = message;
        value.actionUrl = actionUrl;
        value.createdAt = OffsetDateTime.now();
        return value;
    }

    public void markRead() {
        if (readAt == null) readAt = OffsetDateTime.now();
    }
}
