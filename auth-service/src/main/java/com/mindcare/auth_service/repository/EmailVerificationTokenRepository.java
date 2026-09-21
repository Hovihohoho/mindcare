package com.mindcare.auth_service.repository;

import com.mindcare.auth_service.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import java.time.Instant;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);
    void deleteByUserId(UUID userId);
    long deleteByExpiresAtBeforeOrUsedAtBefore(Instant expiredBefore, Instant usedBefore);
}
