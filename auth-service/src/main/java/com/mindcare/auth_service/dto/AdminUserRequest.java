package com.mindcare.auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AdminUserRequest {
    public record CreateExpert(
            @NotBlank String fullName,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72) String password) {}

    public record Update(
            @NotBlank String fullName,
            @NotBlank @Email String email,
            @NotBlank String role,
            boolean active) {}

    public record ExpertReview(
            boolean approved,
            @Size(max = 500) String reason) {}
}
