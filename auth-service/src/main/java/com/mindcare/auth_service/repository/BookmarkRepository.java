package com.mindcare.auth_service.repository;

import com.mindcare.auth_service.entity.Bookmark;
import com.mindcare.auth_service.entity.BookmarkType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookmarkRepository extends JpaRepository<Bookmark, UUID> {
    List<Bookmark> findByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<Bookmark> findByUserIdAndTargetTypeAndTargetId(UUID userId, BookmarkType type, String targetId);
}
