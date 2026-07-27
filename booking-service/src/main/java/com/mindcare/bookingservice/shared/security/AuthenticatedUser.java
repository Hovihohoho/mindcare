package com.mindcare.bookingservice.shared.security;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, UserRole role) {
}
