package com.mindcare.ai_service.service;

import com.mindcare.ai_service.dto.RagChatRequest;
import com.mindcare.ai_service.dto.RagChatResponse;
import com.mindcare.ai_service.exception.ChatRequestException;
import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** One transaction serializes each user's writes across REST, WebSocket and service replicas. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatExecutionService {
    private final JdbcTemplate jdbc;
    private final AiConversationService conversations;
    private final ObjectMapper mapper;
    private final Validator validator;
    @Value("${ai.chat.requests-per-minute:12}") private int requestsPerMinute = 12;

    @Transactional
    public RagChatResponse chat(UUID userId, RagChatRequest request) {
        if (!validator.validate(request).isEmpty()) {
            throw new ChatRequestException(HttpStatus.BAD_REQUEST, "Câu hỏi không hợp lệ.");
        }
        long started = System.nanoTime();
        String outcome = "error";
        try {
            // No unbounded queue or repeated provider call while another request is running.
            Boolean locked = jdbc.queryForObject(
                    "SELECT pg_try_advisory_xact_lock(hashtextextended(?, 0))", Boolean.class,
                    "mindcare-ai:" + userId);
            if (!Boolean.TRUE.equals(locked)) {
                throw new ChatRequestException(HttpStatus.TOO_MANY_REQUESTS,
                        "Một câu hỏi đang được xử lý. Hãy chờ rồi thử lại với cùng mã yêu cầu.");
            }
            UUID requestId = request.requestId() == null ? UUID.randomUUID() : request.requestId();
            String fingerprint = fingerprint(request);
            var existing = jdbc.query(
                    "SELECT fingerprint, response_json FROM ai_schema.ai_chat_requests WHERE user_id=? AND request_id=?",
                    (rs, row) -> new Cached(rs.getString(1), rs.getString(2)), userId, requestId);
            if (!existing.isEmpty()) {
                Cached cached = existing.get(0);
                if (!cached.fingerprint().equals(fingerprint)) {
                    throw new ChatRequestException(HttpStatus.CONFLICT,
                            "Mã yêu cầu đã được sử dụng cho nội dung khác.");
                }
                outcome = "replay";
                return mapper.readValue(cached.json(), RagChatResponse.class);
            }
            var admitted = jdbc.queryForList("""
                    INSERT INTO ai_schema.ai_chat_rate_limits (user_id, window_started_at, request_count)
                    VALUES (?, CURRENT_TIMESTAMP, 1)
                    ON CONFLICT (user_id) DO UPDATE SET
                        window_started_at = CASE WHEN ai_chat_rate_limits.window_started_at <= CURRENT_TIMESTAMP - INTERVAL '1 minute'
                            THEN CURRENT_TIMESTAMP ELSE ai_chat_rate_limits.window_started_at END,
                        request_count = CASE WHEN ai_chat_rate_limits.window_started_at <= CURRENT_TIMESTAMP - INTERVAL '1 minute'
                            THEN 1 ELSE ai_chat_rate_limits.request_count + 1 END
                    WHERE ai_chat_rate_limits.window_started_at <= CURRENT_TIMESTAMP - INTERVAL '1 minute'
                        OR ai_chat_rate_limits.request_count < ?
                    RETURNING request_count
                    """, Integer.class, userId, Math.max(1, requestsPerMinute));
            if (admitted.isEmpty()) {
                throw new ChatRequestException(HttpStatus.TOO_MANY_REQUESTS,
                        "Bạn gửi câu hỏi quá nhanh. Vui lòng thử lại sau một phút.");
            }
            RagChatResponse response = conversations.chat(userId, request);
            jdbc.update("""
                    INSERT INTO ai_schema.ai_chat_requests
                    (user_id, request_id, fingerprint, conversation_id, response_json)
                    VALUES (?, ?, ?, ?, ?)
                    """, userId, requestId, fingerprint, response.conversationId(), mapper.writeValueAsString(response));
            outcome = "completed";
            return response;
        } finally {
            log.info("ai_chat outcome={} duration_ms={}", outcome, (System.nanoTime() - started) / 1_000_000);
        }
    }

    private String fingerprint(RagChatRequest request) {
        try {
            // Client history is ignored by conversation persistence and is intentionally excluded.
            String canonical = mapper.writeValueAsString(java.util.Arrays.asList(
                    request.question(), request.topK(), request.conversationId()));
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private record Cached(String fingerprint, String json) {}

    @org.springframework.scheduling.annotation.Scheduled(fixedDelay = 3_600_000)
    public void removeExpiredRateCounters() {
        jdbc.update("DELETE FROM ai_schema.ai_chat_rate_limits WHERE window_started_at < CURRENT_TIMESTAMP - INTERVAL '1 day'");
    }
}
