package com.mindcare.bookingservice.schedule.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record CreateScheduleRequest(
        @NotNull @Future OffsetDateTime startAt,
        @NotNull @Future OffsetDateTime endAt) {
}
