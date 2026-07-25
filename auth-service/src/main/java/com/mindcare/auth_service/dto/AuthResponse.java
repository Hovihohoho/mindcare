package com.mindcare.auth_service.dto;

public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserSummary user) {
    public static AuthResponse bearer(String token, long expiresIn, UserSummary user) {
        return new AuthResponse(token, "Bearer", expiresIn, user);
    }
}
