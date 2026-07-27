package com.mindcare.bookingservice.schedule.service;

import com.mindcare.bookingservice.schedule.entity.ScheduleStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ScheduleSnapshot(
        UUID id,
        UUID expertUserId,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        ScheduleStatus status,
        OffsetDateTime holdExpiresAt) {
}
