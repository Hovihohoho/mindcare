package com.mindcare.emotionservice.shared.util;

import com.mindcare.emotionservice.shared.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CursorCodecTest {

    private final CursorCodec codec = new CursorCodec();

    @Test
    void cursorRoundTripPreservesPosition() {
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-07-22T00:00:00Z");
        UUID id = UUID.randomUUID();

        String cursor = codec.encode(timestamp, id, "journal-scope");
        CursorCodec.CursorPosition decoded = codec.decode(cursor, "journal-scope");

        assertEquals(timestamp, decoded.timestamp());
        assertEquals(id, decoded.id());
    }

    @Test
    void cursorCannotBeReusedForDifferentFilterScope() {
        String cursor = codec.encode(
                OffsetDateTime.parse("2026-07-22T00:00:00Z"),
                UUID.randomUUID(),
                "scope-a"
        );

        assertThrows(InvalidRequestException.class, () -> codec.decode(cursor, "scope-b"));
    }
}
