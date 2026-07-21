package com.mindcare.auth_service.controller;

import com.mindcare.auth_service.dto.ApiResponse;
import com.mindcare.auth_service.dto.UserSummary;
import com.mindcare.auth_service.entity.Role;
import com.mindcare.auth_service.entity.User;
import com.mindcare.auth_service.repository.RoleRepository;
import com.mindcare.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/auth/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @GetMapping
    public ApiResponse<List<UserSummary>> findUsers() {
        return ApiResponse.success("Danh sách người dùng",
                userRepository.findAll().stream().map(UserSummary::from).toList());
    }

    @GetMapping("/roles")
    public ApiResponse<List<Role>> findRoles() {
        return ApiResponse.success("Danh sách vai trò", roleRepository.findAll());
    }

    @PatchMapping("/{userId}/expert")
    public ResponseEntity<ApiResponse<Void>> grantExpert(@PathVariable UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        Role expert = roleRepository.findByName("ROLE_EXPERT")
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ROLE_EXPERT"));
        user.setRole(expert);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("Đã cấp quyền chuyên gia", null));
    }
}
