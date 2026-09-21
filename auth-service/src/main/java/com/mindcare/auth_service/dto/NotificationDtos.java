package com.mindcare.auth_service.dto;

import com.mindcare.auth_service.entity.Notification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class NotificationDtos {
    private NotificationDtos() {}

    public record Item(UUID id, String type, String title, String message, String actionUrl,
                       OffsetDateTime readAt, OffsetDateTime createdAt) {
        public static Item from(Notification value) {
            return new Item(value.getId(), value.getType(), value.getTitle(), value.getMessage(),
                    value.getActionUrl(), value.getReadAt(), value.getCreatedAt());
        }
    }

    public record Page(List<Item> items, int page, int size, long totalElements,
                       int totalPages, long unreadCount) {}

    public record InternalCreate(@NotNull UUID eventId, @NotNull UUID userId,
                                 @NotBlank @Size(max = 40) String type,
                                 @NotBlank @Size(max = 160) String title,
                                 @NotBlank @Size(max = 500) String message,
                                 @Size(max = 500) String actionUrl) {}

    public record Broadcast(@NotBlank @Size(max = 160) String title,
                            @NotBlank @Size(max = 500) String message,
                            @Size(max = 500) String actionUrl) {}
}
