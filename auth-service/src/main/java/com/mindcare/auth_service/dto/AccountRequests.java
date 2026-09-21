package com.mindcare.auth_service.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public final class AccountRequests {
    private AccountRequests() {}

    public record UpdateProfile(
            @NotBlank @Size(max = 100) String fullName,
            @Size(max = 30) String phone,
            @Past LocalDate birthDate,
            @Pattern(regexp = "MALE|FEMALE|OTHER|", message = "Giới tính không hợp lệ") String gender,
            @Size(max = 500) String address,
            @Size(max = 5000) String bio
    ) {}

    public record ChangePassword(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 72) String newPassword
    ) {}

    public record ConfirmPassword(@NotBlank String currentPassword) {}

    public record ForgotPassword(@NotBlank @Email String email) {}
    public record ResetPassword(
            @NotBlank String token,
            @NotBlank @Size(min = 8, max = 72) String newPassword
    ) {}
}
