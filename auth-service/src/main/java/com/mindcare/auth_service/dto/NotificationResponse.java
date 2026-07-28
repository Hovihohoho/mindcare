package com.mindcare.auth_service.dto;

import com.mindcare.auth_service.entity.Notification;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id, String title, String content, String notificationType,
        String actionUrl, boolean read, Instant createdAt
) {
    public static NotificationResponse from(Notification item) {
        return new NotificationResponse(
                item.getId(), item.getTitle(), item.getContent(), item.getNotificationType(),
                item.getActionUrl(), item.getReadAt() != null, item.getCreatedAt()
        );
    }
}
