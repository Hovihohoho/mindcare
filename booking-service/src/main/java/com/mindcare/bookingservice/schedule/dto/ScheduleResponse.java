package com.mindcare.bookingservice.schedule.dto;

import com.mindcare.bookingservice.schedule.entity.ScheduleStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ScheduleResponse(
        UUID id,
        UUID expertUserId,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        ScheduleStatus status,
        OffsetDateTime holdExpiresAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
