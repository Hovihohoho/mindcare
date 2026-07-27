package com.mindcare.bookingservice.review.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID bookingId,
        UUID expertUserId,
        short rating,
        String comment,
        OffsetDateTime createdAt) {
}
