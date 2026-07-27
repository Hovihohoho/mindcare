package com.mindcare.bookingservice.shared.web;

import com.mindcare.bookingservice.shared.dto.PageCursor;
import com.mindcare.bookingservice.shared.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class CursorCodec {

    public String encode(OffsetDateTime createdAt, UUID id) {
        String raw = createdAt + "|" + id;
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public PageCursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return new PageCursor(null, null);
        }
        try {
            String raw = new String(
                    Base64.getUrlDecoder().decode(cursor),
                    StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|", 2);
            return new PageCursor(
                    OffsetDateTime.parse(parts[0]),
                    UUID.fromString(parts[1]));
        } catch (RuntimeException invalid) {
            throw new BusinessException(
                    "INVALID_CURSOR",
                    HttpStatus.BAD_REQUEST,
                    "Pagination cursor is invalid");
        }
    }
}
