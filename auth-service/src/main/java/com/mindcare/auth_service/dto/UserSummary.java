package com.mindcare.auth_service.dto;

import com.mindcare.auth_service.entity.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserSummary(
        UUID id, String email, String fullName, String role,
        boolean active, boolean emailVerified, LocalDateTime createdAt,
        String avatarUrl, String phone, LocalDate birthDate, String gender,
        String address, String bio
) {
    public static UserSummary from(User user) {
        return new UserSummary(
                user.getId(), user.getEmail(), user.getFullName(), user.getRole().getName(),
                Boolean.TRUE.equals(user.getIsActive()), Boolean.TRUE.equals(user.getEmailVerified()),
                user.getCreatedAt(), user.getAvatarUrl(), user.getPhone(), user.getBirthDate(),
                user.getGender(), user.getAddress(), user.getBio()
        );
    }
}
