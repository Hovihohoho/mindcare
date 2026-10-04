package com.mindcare.auth_service.repository;

import com.mindcare.auth_service.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
import java.time.OffsetDateTime;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE UserSession s SET s.lastSeenAt=:now WHERE s.id=:id AND s.revokedAt IS NULL AND (s.lastSeenAt IS NULL OR s.lastSeenAt < :cutoff)")
    int touchIfStale(@org.springframework.data.repository.query.Param("id") UUID id,
            @org.springframework.data.repository.query.Param("now") OffsetDateTime now,
            @org.springframework.data.repository.query.Param("cutoff") OffsetDateTime cutoff);
    List<UserSession> findByUserIdOrderByCreatedAtDesc(UUID userId);
    long deleteByExpiresAtBeforeOrRevokedAtBefore(OffsetDateTime expiredBefore, OffsetDateTime revokedBefore);
}
