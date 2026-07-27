package com.mindcare.emotionservice.shared.security;

import java.security.Principal;
import java.util.Objects;
import java.util.UUID;

public record AuthenticatedUser(UUID userId, UserRole role) implements Principal {

    public AuthenticatedUser {
        Objects.requireNonNull(userId, "userId must not be null");
    }

    public AuthenticatedUser(UUID userId) {
        this(userId, null);
    }

    @Override
    public String getName() {
        return userId.toString();
    }
}
