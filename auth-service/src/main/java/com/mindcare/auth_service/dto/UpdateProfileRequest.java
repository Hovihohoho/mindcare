package com.mindcare.auth_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) String fullName,
        @Size(max = 30) String phone,
        LocalDate birthDate,
        @Size(max = 30) String gender,
        @Size(max = 500) String address,
        @Size(max = 5000) String bio,
        @Size(max = 255) String headline,
        @Size(max = 2000) String specialties,
        @Min(0) Integer yearsOfExperience,
        @PositiveOrZero BigDecimal consultationFee,
        @Size(max = 255) String workplace,
        @Size(max = 3000) String education
) {}
