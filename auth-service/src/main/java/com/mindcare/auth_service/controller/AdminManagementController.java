package com.mindcare.auth_service.controller;

import com.mindcare.auth_service.dto.AccountRequests;
import com.mindcare.auth_service.dto.ApiResponse;
import com.mindcare.auth_service.dto.UserSummary;
import com.mindcare.auth_service.entity.AdminAuditLog;
import com.mindcare.auth_service.entity.Role;
import com.mindcare.auth_service.entity.User;
import com.mindcare.auth_service.repository.AdminAuditLogRepository;
import com.mindcare.auth_service.repository.ExpertDocumentRepository;
import com.mindcare.auth_service.repository.RoleRepository;
import com.mindcare.auth_service.repository.UserRepository;
import com.mindcare.auth_service.service.AccountService;
import com.mindcare.auth_service.service.AuditService;
import com.mindcare.auth_service.service.ExpertReviewService;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/admin/management")
@RequiredArgsConstructor
public class AdminManagementController {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AdminAuditLogRepository auditRepository;
    private final ExpertDocumentRepository documentRepository;
    private final AuditService auditService;
    private final AccountService accountService;
    private final ExpertReviewService expertReviewService;

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Long>> dashboard() {
        return ApiResponse.success("Thống kê quản trị", Map.of(
                "totalUsers", userRepository.count(),
                "activeUsers", userRepository.countByIsActiveTrue(),
                "experts", userRepository.countByRoleName("ROLE_EXPERT"),
                "pendingExperts", userRepository.countByExpertStatus("PENDING")));
    }

    @GetMapping("/users")
    public ApiResponse<Page<UserSummary>> users(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> result = search.isBlank() ? userRepository.findAll(pageable)
                : userRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        search.trim(), search.trim(), pageable);
        return ApiResponse.success("Danh sách người dùng", result.map(UserSummary::from));
    }

    @PatchMapping("/users/{id}/active")
    public ApiResponse<UserSummary> active(Authentication auth, @PathVariable UUID id,
                                            @RequestBody ActiveRequest request) {
        User user = find(id);
        if (user.getEmail().equalsIgnoreCase(auth.getName()) && !request.active()) {
            throw new RuntimeException("Admin không thể tự khóa tài khoản đang sử dụng");
        }
        user.setIsActive(request.active());
        User saved = userRepository.save(user);
        if (!request.active()) accountService.revokeAllSessions(id);
        auditService.record(auth.getName(), request.active() ? "UNLOCK_USER" : "LOCK_USER",
                "USER", id.toString(), null);
        return ApiResponse.success("Đã cập nhật trạng thái", UserSummary.from(saved));
    }

    @PatchMapping("/users/{id}/role")
    public ApiResponse<UserSummary> role(Authentication auth, @PathVariable UUID id,
                                          @RequestBody RoleRequest request) {
        Role role = roleRepository.findByName(request.role())
                .orElseThrow(() -> new RuntimeException("Vai trò không hợp lệ"));
        User user = find(id);
        if (user.getEmail().equalsIgnoreCase(auth.getName()) && !"ROLE_ADMIN".equals(request.role())) {
            throw new RuntimeException("Admin không thể tự hạ quyền tài khoản đang sử dụng");
        }
        user.setRole(role);
        User saved = userRepository.save(user);
        accountService.revokeAllSessions(id);
        auditService.record(auth.getName(), "CHANGE_ROLE", "USER", id.toString(), request.role());
        return ApiResponse.success("Đã cập nhật vai trò", UserSummary.from(saved));
    }

    @DeleteMapping("/users/{id}")
    @Transactional
    public ApiResponse<Void> deleteUser(Authentication auth, @PathVariable UUID id) {
        User user = find(id);
        if (user.getEmail().equalsIgnoreCase(auth.getName())) {
            throw new RuntimeException("Admin không thể tự xóa tài khoản đang sử dụng");
        }
        String detail = user.getEmail() + " (" + user.getRole().getName() + ")";
        auditService.record(auth.getName(), "DELETE_USER", "USER", id.toString(), detail);
        userRepository.delete(user);
        userRepository.flush();
        return ApiResponse.success("Đã xóa người dùng", null);
    }

    @GetMapping("/experts")
    public ApiResponse<Page<UserSummary>> experts(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success("Hồ sơ chuyên gia",
                userRepository.findByExpertStatus(status,
                        PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                                Sort.by(Sort.Direction.DESC, "expertSubmittedAt"))).map(UserSummary::from));
    }

    @PatchMapping("/experts/{id}/review")
    public ApiResponse<UserSummary> review(Authentication auth, @PathVariable UUID id,
            @Valid @RequestBody AccountRequests.ExpertReviewRequest request) {
        User saved = expertReviewService.reviewPending(
                id, request.status(), request.reason(), auth.getName());
        return ApiResponse.success("Đã lưu kết quả duyệt hồ sơ", UserSummary.from(saved));
    }

    @GetMapping("/experts/{id}")
    public ApiResponse<ExpertAccountController.ExpertProfileResponse> expert(@PathVariable UUID id) {
        User user = find(id);
        return ApiResponse.success("Chi tiết hồ sơ chuyên gia",
                new ExpertAccountController.ExpertProfileResponse(
                        UserSummary.from(user),
                        documentRepository.findByUserIdOrderByCreatedAtDesc(id).stream()
                                .map(ExpertAccountController.DocumentResponse::from).toList()));
    }

    @GetMapping("/audit-logs")
    public ApiResponse<Page<AuditResponse>> auditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return ApiResponse.success("Nhật ký quản trị",
                auditRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(
                        Math.max(page, 0), Math.min(Math.max(size, 1), 100))).map(AuditResponse::from));
    }

    private User find(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }

    public record ActiveRequest(boolean active) {}
    public record RoleRequest(String role) {}
    public record AuditResponse(UUID id, String adminEmail, String action, String targetType,
                                String targetId, String detail, OffsetDateTime createdAt) {
        static AuditResponse from(AdminAuditLog log) {
            return new AuditResponse(log.getId(), log.getAdmin() == null ? null : log.getAdmin().getEmail(),
                    log.getAction(), log.getTargetType(), log.getTargetId(), log.getDetail(), log.getCreatedAt());
        }
    }
}
