package com.mindcare.ai_service.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mindcare.ai_service.exception.ChatRequestException;

@Service
@RequiredArgsConstructor
public class AccountErasureService {
    private final JdbcTemplate jdbc;
    private final AiConversationService conversations;

    public void requireActive(UUID userId) {
        if (Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM ai_schema.erased_accounts WHERE account_key=?)",
                Boolean.class, key(userId)))) {
            throw new ChatRequestException(HttpStatus.GONE, "Tài khoản đã yêu cầu xóa dữ liệu.");
        }
    }

    @Transactional
    public void erase(UUID userId) {
        jdbc.execute("SET LOCAL lock_timeout = '5s'");
        jdbc.queryForObject("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", Object.class,
                "mindcare-ai:" + userId);
        jdbc.update("INSERT INTO ai_schema.erased_accounts(account_key) VALUES (?) ON CONFLICT DO NOTHING", key(userId));
        conversations.deleteAll(userId);
        jdbc.update("DELETE FROM ai_schema.ai_chat_rate_limits WHERE user_id=?", userId);
    }

    private String key(UUID userId) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(("mindcare-erased:" + userId).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
