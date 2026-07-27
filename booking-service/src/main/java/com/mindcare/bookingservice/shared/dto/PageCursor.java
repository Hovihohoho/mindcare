package com.mindcare.bookingservice.shared.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PageCursor(OffsetDateTime createdAt, UUID id) {
}
