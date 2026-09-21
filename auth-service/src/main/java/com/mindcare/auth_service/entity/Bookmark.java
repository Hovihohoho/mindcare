package com.mindcare.auth_service.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "bookmarks", schema = "auth_schema")
@NoArgsConstructor
public class Bookmark {
    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    private BookmarkType targetType;
    @Column(name = "target_id", nullable = false, length = 120) private String targetId;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;

    public static Bookmark create(UUID userId, BookmarkType type, String targetId) {
        Bookmark value = new Bookmark();
        value.id = UUID.randomUUID();
        value.userId = userId;
        value.targetType = type;
        value.targetId = targetId;
        value.createdAt = OffsetDateTime.now();
        return value;
    }
}
