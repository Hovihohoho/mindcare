package com.mindcare.auth_service.repository;

import com.mindcare.auth_service.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
import java.time.OffsetDateTime;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
    long deleteByUserId(UUID userId);
    long deleteByExpiresAtBeforeOrUsedAtBefore(OffsetDateTime expiredBefore, OffsetDateTime usedBefore);
}
