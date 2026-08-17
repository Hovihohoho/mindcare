package com.mindcare.emotionservice.shared.security;

import java.util.Locale;

public enum UserRole {
    ROLE_USER,
    ROLE_ADMIN;

    public static UserRole parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("X-User-Role contains an unsupported role", exception);
        }
    }
}
