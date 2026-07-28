package com.mindcare.ai_service.security;

import java.security.Principal;
import java.util.UUID;

public record AuthenticatedUser(UUID userId, String role) implements Principal {
    @Override
    public String getName() {
        return userId.toString();
    }
}
