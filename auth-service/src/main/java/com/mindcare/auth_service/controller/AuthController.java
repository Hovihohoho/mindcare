package com.mindcare.auth_service.controller;

import com.mindcare.auth_service.dto.*;
import com.mindcare.auth_service.entity.UserSession;
import com.mindcare.auth_service.service.AccountService;
import com.mindcare.auth_service.service.AuthService;
import com.mindcare.auth_service.service.EmailVerificationService;
import com.mindcare.auth_service.security.JwtUtil;
import com.mindcare.auth_service.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final AccountService accountService;
    private final EmailVerificationService emailVerificationService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody AuthRequest.Register request) {
        authService.register(request);
        return ApiResponse.success("Đăng ký thành công", null);
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody AuthRequest.Login request,
                                           HttpServletRequest servletRequest) {
        return ApiResponse.success("Đăng nhập thành công",
                authService.login(request, servletRequest.getHeader("User-Agent"),
                        servletRequest.getRemoteAddr()));
    }

    @GetMapping("/me")
    public ApiResponse<UserSummary> me(Authentication authentication) {
        return ApiResponse.success("Thông tin tài khoản",
                UserSummary.from(accountService.current(authentication.getName())));
    }

    @PutMapping("/me")
    public ApiResponse<UserSummary> updateMe(Authentication authentication,
                                              @Valid @RequestBody AccountRequests.UpdateProfile request) {
        return ApiResponse.success("Cập nhật hồ sơ thành công",
                accountService.updateProfile(authentication.getName(), request));
    }

    @PutMapping("/me/password")
    public ApiResponse<Void> changePassword(Authentication authentication,
                                             @Valid @RequestBody AccountRequests.ChangePassword request) {
        accountService.changePassword(authentication.getName(), request);
        return ApiResponse.success("Đổi mật khẩu thành công", null);
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> deactivate(Authentication authentication) {
        accountService.deactivate(authentication.getName());
        return ApiResponse.success("Tài khoản đã được vô hiệu hóa", null);
    }

    @GetMapping("/sessions")
    public ApiResponse<List<SessionResponse>> sessions(Authentication authentication) {
        return ApiResponse.success("Danh sách phiên đăng nhập",
                accountService.sessions(authentication.getName()).stream().map(SessionResponse::from).toList());
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ApiResponse<Void> revokeSession(Authentication authentication, @PathVariable UUID sessionId) {
        accountService.revokeSession(authentication.getName(), sessionId);
        return ApiResponse.success("Đã đăng xuất phiên", null);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(Authentication authentication, HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            accountService.revokeSession(authentication.getName(),
                    jwtUtil.extractSessionId(authorization.substring(7)));
        }
        return ApiResponse.success("Đăng xuất thành công", null);
    }

    @PostMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(@Valid @RequestBody VerifyEmail request) {
        emailVerificationService.verify(request.email(), request.code());
        return ApiResponse.success("Xác thực email thành công", null);
    }

    @PostMapping("/resend-verification")
    public ApiResponse<Void> resendVerification(@Valid @RequestBody ResendVerification request) {
        emailVerificationService.resend(request.email());
        return ApiResponse.success("Nếu email phù hợp, thư xác thực mới đã được gửi", null);
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody AccountRequests.ForgotPassword request) {
        accountService.forgotPassword(request.email());
        return ApiResponse.success("Nếu email tồn tại, hướng dẫn đặt lại mật khẩu đã được gửi", null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody AccountRequests.ResetPassword request) {
        accountService.resetPassword(request);
        return ApiResponse.success("Đặt lại mật khẩu thành công", null);
    }

    public record ResendVerification(
            @jakarta.validation.constraints.NotBlank
            @jakarta.validation.constraints.Email String email) {}
    public record VerifyEmail(
            @jakarta.validation.constraints.NotBlank
            @jakarta.validation.constraints.Email String email,
            @jakarta.validation.constraints.NotBlank
            @jakarta.validation.constraints.Pattern(regexp = "\\d{6}", message = "Mã xác thực phải gồm đúng 6 chữ số")
            String code) {}
    public record SessionResponse(UUID id, String userAgent, String ipAddress,
                                  OffsetDateTime createdAt, OffsetDateTime lastSeenAt,
                                  OffsetDateTime expiresAt, boolean revoked) {
        static SessionResponse from(UserSession item) {
            return new SessionResponse(item.getId(), item.getUserAgent(), item.getIpAddress(),
                    item.getCreatedAt(), item.getLastSeenAt(), item.getExpiresAt(),
                    item.getRevokedAt() != null);
        }
    }

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
                && "ROLE_EXPERT".equals(user.getRole().getName())
                && "APPROVED".equals(user.getExpertStatus());
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
                .filter(user -> "APPROVED".equals(user.getExpertStatus()))
                .map(user -> new InternalExpert(user.getId(), user.getFullName(),
                        user.getHeadline(), user.getSpecialties(), user.getYearsOfExperience(),
                        user.getConsultationFee() == null ? defaultFee : user.getConsultationFee(), "VND",
                        user.getAvatarUrl(), user.getBio(), user.getWorkplace(), user.getEducation()))
                .toList();
    }

    public record InternalExpert(UUID expertUserId, String displayName, String headline,
                                 String specialties, Integer yearsOfExperience,
                                 BigDecimal consultationFee, String currency, String avatarUrl,
                                 String bio, String workplace, String education) {}
}
