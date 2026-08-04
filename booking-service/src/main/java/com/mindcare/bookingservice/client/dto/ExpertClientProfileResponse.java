package com.mindcare.bookingservice.client.dto;

import com.mindcare.bookingservice.booking.entity.BookingStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record ExpertClientProfileResponse(
        UUID userId,
        String fullName,
        String email,
        LocalDateTime createdAt,
        UUID bookingId,
        UUID scheduleId,
        BookingStatus bookingStatus) {
}
