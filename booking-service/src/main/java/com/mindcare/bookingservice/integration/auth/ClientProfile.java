package com.mindcare.bookingservice.integration.auth;

import java.time.LocalDateTime;
import java.util.UUID;

public record ClientProfile(
        UUID userId,
        String fullName,
        String email,
        LocalDateTime createdAt) {
}
