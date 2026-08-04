package com.mindcare.auth_service.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class AccountRequests {
    private AccountRequests() {}

    public record UpdateProfile(
            @NotBlank @Size(max = 100) String fullName,
            @Size(max = 30) String phone,
            @Past LocalDate birthDate,
            @Pattern(regexp = "MALE|FEMALE|OTHER|", message = "Giới tính không hợp lệ") String gender,
            @Size(max = 500) String address,
            @Size(max = 5000) String bio,
            @Size(max = 255) String headline,
            @Size(max = 1000) String specialties,
            @Min(0) @Max(80) Integer yearsOfExperience,
            @DecimalMin("0") BigDecimal consultationFee,
            @Size(max = 255) String workplace,
            @Size(max = 3000) String education
    ) {}

    public record ChangePassword(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 72) String newPassword
    ) {}

    public record ForgotPassword(@NotBlank @Email String email) {}
    public record ResetPassword(
            @NotBlank String token,
            @NotBlank @Size(min = 8, max = 72) String newPassword
    ) {}
    public record ExpertDocumentRequest(
            @NotBlank @Size(max = 50) String documentType,
            @NotBlank @Size(max = 255) String title,
            @NotBlank @Size(max = 500) String fileUrl
    ) {}
    public record ExpertReviewRequest(
            @NotBlank @Pattern(regexp = "APPROVED|REJECTED") String status,
            @Size(max = 3000) String reason
    ) {}
}
