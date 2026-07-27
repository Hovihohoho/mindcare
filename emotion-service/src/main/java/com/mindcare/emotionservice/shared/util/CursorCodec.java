package com.mindcare.emotionservice.shared.util;

import com.mindcare.emotionservice.shared.exception.InvalidRequestException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

@Component
public class CursorCodec {

    private static final String VERSION = "v1";

    public CursorPosition decode(String cursor, String scope) {
        if (cursor == null || cursor.isBlank()) {
            return new CursorPosition(null, null);
        }
        try {
            String payload = new String(
                    Base64.getUrlDecoder().decode(cursor),
                    StandardCharsets.UTF_8
            );
            String[] parts = payload.split("\\|", -1);
            if (parts.length != 4
                    || !VERSION.equals(parts[0])
                    || !scopeFingerprint(scope).equals(parts[1])) {
                throw invalidCursor();
            }
            return new CursorPosition(OffsetDateTime.parse(parts[2]), UUID.fromString(parts[3]));
        } catch (IllegalArgumentException exception) {
            throw invalidCursor();
        }
    }

    public String encode(OffsetDateTime timestamp, UUID id, String scope) {
        String payload = String.join(
                "|",
                VERSION,
                scopeFingerprint(scope),
                timestamp.toString(),
                id.toString()
        );
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    private String scopeFingerprint(String scope) {
        return RequestHasher.sha256(scope).substring(0, 16);
    }

    private InvalidRequestException invalidCursor() {
        return new InvalidRequestException("INVALID_CURSOR", "cursor is invalid for this query");
    }

    public record CursorPosition(OffsetDateTime timestamp, UUID id) {
    }
}
