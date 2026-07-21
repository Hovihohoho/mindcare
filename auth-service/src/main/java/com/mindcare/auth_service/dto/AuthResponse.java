package com.mindcare.auth_service.dto;

public record AuthResponse(String accessToken, String tokenType, long expiresIn) {
    public static AuthResponse bearer(String token, long expiresIn) {
        return new AuthResponse(token, "Bearer", expiresIn);
    }
}
