package com.mindcare.bookingservice.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mindcare.bookingservice.shared.exception.BusinessException;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CursorCodecTest {

    private final CursorCodec cursorCodec = new CursorCodec();

    @Test
    void cursorRoundTrips() {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-08-01T08:00:00Z");
        UUID id = UUID.randomUUID();

        var decoded = cursorCodec.decode(cursorCodec.encode(createdAt, id));

        assertThat(decoded.createdAt()).isEqualTo(createdAt);
        assertThat(decoded.id()).isEqualTo(id);
    }

    @Test
    void invalidCursorIsRejected() {
        assertThatThrownBy(() -> cursorCodec.decode("not-a-cursor"))
                .isInstanceOf(BusinessException.class);
    }
}
