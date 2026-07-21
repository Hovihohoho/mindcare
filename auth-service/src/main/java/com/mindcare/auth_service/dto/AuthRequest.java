package com.mindcare.auth_service.dto;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthRequest {

    @Data
    public static class Login {
        @NotBlank
        @Email
        private String email;
        @NotBlank
        private String password;
    }

    @Data
    public static class Register {
        @NotBlank
        private String fullName;
        @NotBlank
        @Email
        private String email;
        @NotBlank
        @Size(min = 8, max = 72)
        private String password;
    }
}
