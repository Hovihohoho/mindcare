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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final AccountService accountService;
    private final EmailVerificationService emailVerificationService;
    private final JwtUtil jwtUtil;

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

    @GetMapping("/me/data-export")
    public ApiResponse<Map<String, Object>> exportData(Authentication authentication) {
        return ApiResponse.success(
                "Dữ liệu tài khoản của bạn",
                accountService.exportData(authentication.getName()));
    }

    @PostMapping("/me/verify-password")
    public ApiResponse<Void> verifyPassword(
            Authentication authentication,
            @Valid @RequestBody AccountRequests.ConfirmPassword request) {
        accountService.verifyPassword(authentication.getName(), request.currentPassword());
        return ApiResponse.success("Mật khẩu hợp lệ", null);
    }

    @DeleteMapping("/me/permanent")
    public ApiResponse<Void> permanentlyDelete(
            Authentication authentication,
            @Valid @RequestBody AccountRequests.ConfirmPassword request) {
        String avatarUrl = accountService.permanentlyDelete(
                authentication.getName(), request.currentPassword());
        accountService.deleteAvatarFile(avatarUrl);
        return ApiResponse.success("Tài khoản đã được xóa vĩnh viễn", null);
    }

}
