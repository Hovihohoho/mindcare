package com.mindcare.auth_service.controller;

import com.mindcare.auth_service.dto.*;
import com.mindcare.auth_service.entity.AdminAuditLog;
import com.mindcare.auth_service.entity.Role;
import com.mindcare.auth_service.entity.User;
import com.mindcare.auth_service.repository.*;
import com.mindcare.auth_service.service.AuditService;
import com.mindcare.auth_service.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth/admin/management")
@RequiredArgsConstructor
public class AdminManagementController {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AdminAuditLogRepository auditRepository;
    private final ExpertDocumentRepository documentRepository;
    private final NotificationRepository notificationRepository;
    private final AuditService auditService;
    private final AccountService accountService;

    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Long>> dashboard() {
        return ApiResponse.success("Thống kê quản trị", Map.of(
                "totalUsers", userRepository.count(),
                "activeUsers", userRepository.countByIsActiveTrue(),
                "experts", userRepository.countByRoleName("ROLE_EXPERT"),
                "pendingExperts", userRepository.countByExpertStatus("PENDING")
        ));
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
                        PageRequest.of(page, Math.min(Math.max(size, 1), 100),
                                Sort.by(Sort.Direction.DESC, "expertSubmittedAt"))).map(UserSummary::from));
    }

    @PatchMapping("/experts/{id}/review")
    public ApiResponse<UserSummary> review(Authentication auth, @PathVariable UUID id,
            @Valid @RequestBody AccountRequests.ExpertReviewRequest request) {
        User user = find(id);
        if (!"PENDING".equals(user.getExpertStatus())) {
            throw new RuntimeException("Hồ sơ không ở trạng thái chờ duyệt");
        }
        user.setExpertStatus(request.status());
        user.setExpertReviewReason(request.reason());
        user.setExpertReviewedAt(OffsetDateTime.now());
        if ("APPROVED".equals(request.status())) {
            user.setRole(roleRepository.findByName("ROLE_EXPERT")
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy ROLE_EXPERT")));
        }
        User saved = userRepository.save(user);
        com.mindcare.auth_service.entity.Notification notification =
                com.mindcare.auth_service.entity.Notification.create(saved.getId(), null, "EXPERT_REVIEW",
                "Kết quả duyệt hồ sơ chuyên gia", "APPROVED".equals(request.status())
                ? "Hồ sơ chuyên gia của bạn đã được duyệt. Vui lòng đăng nhập lại để sử dụng quyền chuyên gia."
                : "Hồ sơ chuyên gia cần được bổ sung. Lý do: " + request.reason(),
                "APPROVED".equals(request.status()) ? "/expert" : "/expert/register");
        notificationRepository.save(notification);
        if ("APPROVED".equals(request.status())) accountService.revokeAllSessions(id);
        auditService.record(auth.getName(), "REVIEW_EXPERT", "USER", id.toString(),
                request.status() + ": " + request.reason());
        return ApiResponse.success("Đã duyệt hồ sơ", UserSummary.from(saved));
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
                auditRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, Math.min(size, 100)))
                        .map(AuditResponse::from));
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
