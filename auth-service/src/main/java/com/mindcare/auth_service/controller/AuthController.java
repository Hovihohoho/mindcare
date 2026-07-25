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
}
