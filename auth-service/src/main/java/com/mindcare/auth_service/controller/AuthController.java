package com.mindcare.auth_service.controller;

import com.mindcare.auth_service.dto.ApiResponse;
import com.mindcare.auth_service.dto.AuthRequest;
import com.mindcare.auth_service.dto.AuthResponse;
import com.mindcare.auth_service.dto.UserSummary;
import com.mindcare.auth_service.repository.UserRepository;
import com.mindcare.auth_service.service.AuthService;
import com.mindcare.auth_service.service.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody AuthRequest.Register request) {
        authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng ký thành công", null));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest.Login request) {
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", authService.login(request)));
    }

    @GetMapping("/me")
    public ApiResponse<UserSummary> me(Authentication authentication) {
        return ApiResponse.success("Thông tin tài khoản", userRepository.findByEmail(authentication.getName())
                .map(UserSummary::from)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng")));
    }

    @GetMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(@RequestParam String token) {
        emailVerificationService.verify(token);
        return ApiResponse.success("Xác thực email thành công", null);
    }

    @PostMapping("/resend-verification")
    public ApiResponse<Void> resendVerification(@RequestBody ResendVerification request) {
        emailVerificationService.resend(request.email());
        return ApiResponse.success("Nếu email tồn tại và chưa xác thực, thư mới đã được gửi", null);
    }

    public record ResendVerification(String email) {}

    @GetMapping("/internal/experts/{userId}/booking-profile")
    public ExpertBookingProfile bookingProfile(
            @PathVariable UUID userId,
            @RequestHeader("X-Internal-Secret") String suppliedSecret,
            @Value("${app.internal-secret}") String expectedSecret,
            @Value("${app.default-consultation-fee:300000}") BigDecimal defaultFee) {
        if (!expectedSecret.equals(suppliedSecret)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        boolean eligible = Boolean.TRUE.equals(user.getIsActive())
                && Boolean.TRUE.equals(user.getEmailVerified())
                && "ROLE_EXPERT".equals(user.getRole().getName());
        return new ExpertBookingProfile(user.getId(), eligible,
                user.getConsultationFee() == null ? defaultFee : user.getConsultationFee(), "VND");
    }

    public record ExpertBookingProfile(UUID expertUserId, boolean eligible,
                                       BigDecimal consultationFee, String currency) {}

    @GetMapping("/internal/users/{userId}/client-profile")
    public InternalClientProfile clientProfile(
            @PathVariable UUID userId,
            @RequestHeader("X-Internal-Secret") String suppliedSecret,
            @Value("${app.internal-secret}") String expectedSecret) {
        if (!expectedSecret.equals(suppliedSecret)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return new InternalClientProfile(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getCreatedAt());
    }

    public record InternalClientProfile(
            UUID userId,
            String fullName,
            String email,
            java.time.LocalDateTime createdAt) {}

    @GetMapping("/internal/experts")
    public List<InternalExpert> internalExperts(
            @RequestHeader("X-Internal-Secret") String suppliedSecret,
            @Value("${app.internal-secret}") String expectedSecret,
            @Value("${app.default-consultation-fee:300000}") BigDecimal defaultFee) {
        if (!expectedSecret.equals(suppliedSecret)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return userRepository.findByRoleNameAndIsActiveTrueOrderByFullNameAsc("ROLE_EXPERT").stream()
                .filter(user -> Boolean.TRUE.equals(user.getEmailVerified()))
                .map(user -> new InternalExpert(user.getId(), user.getFullName(),
                        user.getHeadline(), user.getSpecialties(), user.getYearsOfExperience(),
                        user.getConsultationFee() == null ? defaultFee : user.getConsultationFee(), "VND"))
                .toList();
    }

    public record InternalExpert(UUID expertUserId, String displayName, String headline,
                                 String specialties, Integer yearsOfExperience,
                                 BigDecimal consultationFee, String currency) {}
}
