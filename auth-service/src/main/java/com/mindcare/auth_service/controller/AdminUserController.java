package com.mindcare.auth_service.controller;

import com.mindcare.auth_service.dto.AdminUserRequest;
import com.mindcare.auth_service.dto.ApiResponse;
import com.mindcare.auth_service.dto.UserSummary;
import com.mindcare.auth_service.entity.Role;
import com.mindcare.auth_service.entity.User;
import com.mindcare.auth_service.repository.RoleRepository;
import com.mindcare.auth_service.repository.UserRepository;
import com.mindcare.auth_service.service.EmailVerificationService;
import com.mindcare.auth_service.service.ExpertReviewService;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final ExpertReviewService expertReviewService;

    @GetMapping
    public ApiResponse<List<UserSummary>> findUsers() {
        return ApiResponse.success("Danh sách người dùng",
                userRepository.findAll().stream().map(UserSummary::from).toList());
    }

    @GetMapping("/roles")
    public ApiResponse<List<Role>> findRoles() {
        return ApiResponse.success("Danh sách vai trò", roleRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserSummary>> createExpert(
            @Valid @RequestBody AdminUserRequest.CreateExpert request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email đã được sử dụng");
        }
        Role expert = roleRepository.findByName("ROLE_EXPERT")
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ROLE_EXPERT"));
        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(expert);
        user.setEmailVerified(false);
        user.setExpertStatus("APPROVED");
        user.setExpertReviewedAt(OffsetDateTime.now());
        User saved = userRepository.save(user);
        emailVerificationService.sendVerification(saved);
        return ResponseEntity.ok(ApiResponse.success(
                "Tạo tài khoản chuyên gia thành công. Email xác thực đã được gửi", UserSummary.from(saved)));
    }

    @PutMapping("/{userId}")
    public ApiResponse<UserSummary> update(Authentication auth, @PathVariable UUID userId,
                                            @Valid @RequestBody AdminUserRequest.Update request) {
        User user = findUser(userId);
        if (user.getEmail().equalsIgnoreCase(auth.getName())
                && (!request.active() || !"ROLE_ADMIN".equals(request.role()))) {
            throw new RuntimeException("Admin không thể tự khóa hoặc tự hạ quyền tài khoản đang sử dụng");
        }
        String email = request.email().trim().toLowerCase();
        userRepository.findByEmail(email)
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> { throw new RuntimeException("Email đã được sử dụng"); });
        Role role = roleRepository.findByName(request.role())
                .orElseThrow(() -> new RuntimeException("Vai trò không hợp lệ"));
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setRole(role);
        user.setIsActive(request.active());
        return ApiResponse.success("Cập nhật người dùng thành công",
                UserSummary.from(userRepository.save(user)));
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Void> delete(Authentication auth, @PathVariable UUID userId) {
        User user = findUser(userId);
        if (user.getEmail().equalsIgnoreCase(auth.getName())) {
            throw new RuntimeException("Admin không thể tự xóa tài khoản đang sử dụng");
        }
        userRepository.delete(user);
        return ApiResponse.success("Xóa người dùng thành công", null);
    }

    @PatchMapping("/{userId}/expert")
    public ResponseEntity<ApiResponse<Void>> grantExpert(
            Authentication auth, @PathVariable UUID userId) {
        expertReviewService.grantExpert(userId, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Đã cấp quyền chuyên gia", null));
    }

    @PostMapping("/{userId}/expert-review")
    public ApiResponse<Void> reviewExpert(Authentication auth, @PathVariable UUID userId,
            @Valid @RequestBody AdminUserRequest.ExpertReview request) {
        expertReviewService.reviewPending(userId,
                request.approved() ? "APPROVED" : "REJECTED", request.reason(), auth.getName());
        return ApiResponse.success("Đã lưu kết quả duyệt chuyên gia", null);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }
}
