package com.mindcare.auth_service.service;

import com.mindcare.auth_service.repository.EmailVerificationTokenRepository;
import com.mindcare.auth_service.repository.PasswordResetTokenRepository;
import com.mindcare.auth_service.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthMaintenanceService {
    private final UserSessionRepository sessionRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final EmailVerificationTokenRepository verificationTokenRepository;

    @Scheduled(cron = "${app.auth-cleanup-cron:0 30 3 * * *}")
    @Transactional
    public void removeExpiredSecurityData() {
        OffsetDateTime retention = OffsetDateTime.now().minusDays(30);
        long sessions = sessionRepository.deleteByExpiresAtBeforeOrRevokedAtBefore(retention, retention);
        long resets = resetTokenRepository.deleteByExpiresAtBeforeOrUsedAtBefore(retention, retention);
        Instant instantRetention = retention.toInstant();
        long verifications = verificationTokenRepository
                .deleteByExpiresAtBeforeOrUsedAtBefore(instantRetention, instantRetention);
        log.info("Removed expired auth data: sessions={}, resetTokens={}, verificationTokens={}",
                sessions, resets, verifications);
    }
}
