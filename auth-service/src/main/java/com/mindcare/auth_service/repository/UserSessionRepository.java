package com.mindcare.auth_service.repository;

import com.mindcare.auth_service.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
import java.time.OffsetDateTime;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    List<UserSession> findByUserIdOrderByCreatedAtDesc(UUID userId);
    long deleteByExpiresAtBeforeOrRevokedAtBefore(OffsetDateTime expiredBefore, OffsetDateTime revokedBefore);
}
