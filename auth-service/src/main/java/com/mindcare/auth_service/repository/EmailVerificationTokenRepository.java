package com.mindcare.auth_service.repository;

import com.mindcare.auth_service.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);
    Optional<EmailVerificationToken> findByUserIdAndTokenHash(UUID userId, String tokenHash);
    void deleteByUserId(UUID userId);
}
