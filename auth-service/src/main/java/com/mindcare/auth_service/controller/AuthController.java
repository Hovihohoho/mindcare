package com.mindcare.auth_service.controller;

import com.mindcare.auth_service.dto.ApiResponse;
import com.mindcare.auth_service.dto.AuthRequest;
import com.mindcare.auth_service.dto.AuthResponse;
import com.mindcare.auth_service.dto.UserSummary;
import com.mindcare.auth_service.dto.UpdateProfileRequest;
import com.mindcare.auth_service.repository.UserRepository;
import com.mindcare.auth_service.service.AuthService;
import com.mindcare.auth_service.service.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @PutMapping("/me")
    public ApiResponse<UserSummary> updateMe(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        var user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        user.setFullName(request.fullName().trim());
        user.setPhone(request.phone());
        user.setBirthDate(request.birthDate());
        user.setGender(request.gender());
        user.setAddress(request.address());
        user.setBio(request.bio());
        if ("ROLE_EXPERT".equals(user.getRole().getName())) {
            user.setHeadline(request.headline());
            user.setSpecialties(request.specialties());
            user.setYearsOfExperience(request.yearsOfExperience());
            user.setConsultationFee(request.consultationFee());
            user.setWorkplace(request.workplace());
            user.setEducation(request.education());
        }
        return ApiResponse.success("Cập nhật hồ sơ thành công", UserSummary.from(userRepository.save(user)));
    }

    @PostMapping("/verify-email")
    public ApiResponse<Void> verifyEmail(@Valid @RequestBody VerifyEmail request) {
        emailVerificationService.verifyCode(request.email(), request.code());
        return ApiResponse.success("Xác thực email thành công", null);
    }

    @PostMapping("/resend-verification")
    public ApiResponse<Void> resendVerification(@RequestBody ResendVerification request) {
        emailVerificationService.resend(request.email());
        return ApiResponse.success("Nếu email tồn tại và chưa xác thực, thư mới đã được gửi", null);
    }

    public record ResendVerification(String email) {}
    public record VerifyEmail(
            @jakarta.validation.constraints.Email String email,
            @jakarta.validation.constraints.Pattern(regexp = "\\d{6}") String code
    ) {}
}
