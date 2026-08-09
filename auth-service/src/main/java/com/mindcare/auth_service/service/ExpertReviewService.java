package com.mindcare.auth_service.service;

import com.mindcare.auth_service.dto.NotificationDtos;
import com.mindcare.auth_service.entity.Notification;
import com.mindcare.auth_service.entity.Role;
import com.mindcare.auth_service.entity.User;
import com.mindcare.auth_service.notification.NotificationSocketHub;
import com.mindcare.auth_service.repository.NotificationRepository;
import com.mindcare.auth_service.repository.RoleRepository;
import com.mindcare.auth_service.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExpertReviewService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationSocketHub notificationSocketHub;
    private final AccountService accountService;
    private final AuditService auditService;

    @Transactional
    public User reviewPending(UUID userId, String status, String reason, String adminEmail) {
        if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) {
            throw new RuntimeException("Trạng thái duyệt không hợp lệ");
        }
        User user = findUser(userId);
        if (!"PENDING".equals(user.getExpertStatus())) {
            throw new RuntimeException("Hồ sơ không ở trạng thái chờ duyệt");
        }
        if ("REJECTED".equals(status) && (reason == null || reason.isBlank())) {
            throw new RuntimeException("Vui lòng nhập lý do từ chối hồ sơ");
        }
        return applyDecision(user, status, reason, adminEmail);
    }

    @Transactional
    public User grantExpert(UUID userId, String adminEmail) {
        return applyDecision(findUser(userId), "APPROVED", null, adminEmail);
    }

    private User applyDecision(User user, String status, String reason, String adminEmail) {
        boolean approved = "APPROVED".equals(status);
        Role targetRole = roleRepository.findByName(approved ? "ROLE_EXPERT" : "ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vai trò phù hợp"));
        String normalizedReason = approved ? null : reason.trim();
        user.setRole(targetRole);
        user.setExpertStatus(status);
        user.setExpertReviewReason(normalizedReason);
        user.setExpertReviewedAt(OffsetDateTime.now());
        User saved = userRepository.save(user);

        Notification notification = notificationRepository.save(Notification.create(
                saved.getId(), null, approved ? "EXPERT_APPROVED" : "EXPERT_REJECTED",
                approved ? "Hồ sơ chuyên gia đã được duyệt" : "Hồ sơ chuyên gia chưa được duyệt",
                approved
                        ? "Bạn đã có thể sử dụng các chức năng dành cho chuyên gia. Vui lòng đăng nhập lại."
                        : "Hồ sơ cần được bổ sung. Lý do: " + normalizedReason,
                approved ? "/expert" : "/expert/register"));
        notificationSocketHub.publish(saved.getId(), NotificationDtos.Item.from(notification));
        accountService.revokeAllSessions(saved.getId());
        if (adminEmail != null && !adminEmail.isBlank()) {
            auditService.record(adminEmail, "REVIEW_EXPERT", "USER", saved.getId().toString(),
                    status + (normalizedReason == null ? "" : ": " + normalizedReason));
        }
        return saved;
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }
}
