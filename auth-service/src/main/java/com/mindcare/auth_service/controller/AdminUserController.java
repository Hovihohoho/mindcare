package com.mindcare.auth_service.controller;

import com.mindcare.auth_service.dto.AdminUserRequest;
import com.mindcare.auth_service.dto.ApiResponse;
import com.mindcare.auth_service.dto.UserSummary;
import com.mindcare.auth_service.entity.Role;
import com.mindcare.auth_service.entity.User;
import com.mindcare.auth_service.repository.RoleRepository;
import com.mindcare.auth_service.repository.UserRepository;
import com.mindcare.auth_service.service.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

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
        User saved = userRepository.save(user);
        emailVerificationService.sendVerification(saved);
        return ResponseEntity.ok(ApiResponse.success("Tạo tài khoản chuyên gia thành công. Email xác thực đã được gửi",
                UserSummary.from(saved)));
    }

    @PutMapping("/{userId}")
    public ApiResponse<UserSummary> update(@PathVariable UUID userId,
                                            @Valid @RequestBody AdminUserRequest.Update request) {
        User user = findUser(userId);
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
    public ApiResponse<Void> delete(@PathVariable UUID userId) {
        userRepository.delete(findUser(userId));
        return ApiResponse.success("Xóa người dùng thành công", null);
    }

    @PatchMapping("/{userId}/expert")
    public ResponseEntity<ApiResponse<Void>> grantExpert(@PathVariable UUID userId) {
        User user = findUser(userId);
        Role expert = roleRepository.findByName("ROLE_EXPERT")
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ROLE_EXPERT"));
        user.setRole(expert);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("Đã cấp quyền chuyên gia", null));
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }
}
