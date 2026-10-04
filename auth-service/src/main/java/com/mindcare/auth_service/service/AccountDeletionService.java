package com.mindcare.auth_service.service;

import com.mindcare.auth_service.entity.User;
import com.mindcare.auth_service.repository.UserRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

/** Durable, idempotent erasure. No database transaction is held during network calls. */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountDeletionService {
    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final RestClient.Builder clientBuilder;
    @Value("${app.internal-secret:}") private String secret;
    @Value("${app.emotion-service-url:http://localhost:8083}") private String emotionUrl;
    @Value("${app.ai-service-url:http://localhost:8084}") private String aiUrl;
    @Value("${app.upload-directory:uploads}") private String uploadDirectory;
    @Value("${app.deletion.worker-enabled:true}") private boolean workerEnabled;

    @Transactional
    public void request(User user) {
        if (secret.length() < 32) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Account erasure is not configured");
        jdbc.update("""
                INSERT INTO auth_schema.account_deletion_jobs(user_id) VALUES (?)
                ON CONFLICT (user_id) DO NOTHING
                """, user.getId());
        user.setIsActive(false);
        users.save(user);
        jdbc.update("UPDATE auth_schema.user_sessions SET revoked_at=CURRENT_TIMESTAMP WHERE user_id=? AND revoked_at IS NULL", user.getId());
        jdbc.update("UPDATE auth_schema.reminder_preferences SET enabled=FALSE WHERE user_id=?", user.getId());
        jdbc.update("UPDATE auth_schema.push_devices SET enabled=FALSE WHERE user_id=?", user.getId());
    }

    @Scheduled(fixedDelayString = "${app.deletion.poll-ms:30000}", initialDelay = 30000)
    public void retryPending() {
        if (!workerEnabled || secret.length() < 32) return;
        var due = jdbc.queryForList("""
                SELECT user_id FROM auth_schema.account_deletion_jobs
                WHERE next_attempt_at <= CURRENT_TIMESTAMP
                  AND (lease_until IS NULL OR lease_until < CURRENT_TIMESTAMP)
                ORDER BY next_attempt_at LIMIT 20
                """, UUID.class);
        due.forEach(this::process);
    }

    public void process(UUID userId) {
        if (secret.length() < 32) return;
        var claims = jdbc.query("""
                UPDATE auth_schema.account_deletion_jobs
                SET lease_until=CURRENT_TIMESTAMP + INTERVAL '2 minutes', attempts=attempts+1
                WHERE user_id=? AND next_attempt_at <= CURRENT_TIMESTAMP
                  AND (lease_until IS NULL OR lease_until < CURRENT_TIMESTAMP)
                RETURNING emotion_deleted, ai_deleted
                """, (rs, row) -> new Progress(rs.getBoolean(1), rs.getBoolean(2)), userId);
        if (claims.isEmpty()) return;
        try {
            Progress progress = claims.get(0);
            if (!progress.emotionDeleted()) {
                erase(emotionUrl + "/api/v1/privacy/data", userId);
                jdbc.update("UPDATE auth_schema.account_deletion_jobs SET emotion_deleted=TRUE WHERE user_id=?", userId);
            }
            if (!progress.aiDeleted()) {
                erase(aiUrl + "/api/ai/internal/privacy/" + userId, userId);
                jdbc.update("UPDATE auth_schema.account_deletion_jobs SET ai_deleted=TRUE WHERE user_id=?", userId);
            }
            var user = users.findById(userId);
            if (user.isPresent()) {
                removeAvatar(user.get().getAvatarUrl());
                // Cascades also remove the completed job; retain no erasure payload or password.
                users.deleteById(userId);
            }
            log.info("account_erasure outcome=completed");
        } catch (Exception failure) {
            jdbc.update("""
                    UPDATE auth_schema.account_deletion_jobs SET lease_until=NULL,
                      next_attempt_at=CURRENT_TIMESTAMP + LEAST(attempts * 30, 3600) * INTERVAL '1 second'
                    WHERE user_id=?
                    """, userId);
            log.warn("account_erasure outcome=retry_pending");
        }
    }

    private void erase(String url, UUID userId) {
        clientBuilder.build().delete().uri(url)
                .header("X-Internal-Secret", secret)
                .header("X-User-Id", userId.toString())
                .header("X-User-Role", "ROLE_USER")
                .retrieve().toBodilessEntity();
    }

    private void removeAvatar(String avatarUrl) throws java.io.IOException {
        if (avatarUrl == null || !avatarUrl.startsWith("/api/auth/files/avatars/")) return;
        Path directory = Path.of(uploadDirectory, "avatars").toAbsolutePath().normalize();
        Path file = directory.resolve(avatarUrl.substring(avatarUrl.lastIndexOf('/') + 1)).normalize();
        if (file.getParent().equals(directory)) Files.deleteIfExists(file);
    }

    private record Progress(boolean emotionDeleted, boolean aiDeleted) {}
}
