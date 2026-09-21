package com.mindcare.auth_service.dto;

import com.mindcare.auth_service.entity.Bookmark;
import com.mindcare.auth_service.entity.BookmarkType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class BookmarkDtos {
    private BookmarkDtos() {}
    public record Create(@NotNull BookmarkType targetType, @NotBlank @Size(max = 120) String targetId) {}
    public record Item(UUID id, BookmarkType targetType, String targetId, OffsetDateTime createdAt) {
        public static Item from(Bookmark value) {
            return new Item(value.getId(), value.getTargetType(), value.getTargetId(), value.getCreatedAt());
        }
    }
}
