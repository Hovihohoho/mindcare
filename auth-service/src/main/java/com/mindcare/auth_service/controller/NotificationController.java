package com.mindcare.auth_service.controller;

import com.mindcare.auth_service.dto.ApiResponse;
import com.mindcare.auth_service.dto.NotificationResponse;
import com.mindcare.auth_service.entity.Notification;
import com.mindcare.auth_service.entity.User;
import com.mindcare.auth_service.repository.NotificationRepository;
import com.mindcare.auth_service.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<List<NotificationResponse>> list(Authentication authentication) {
        User user = currentUser(authentication);
        return ApiResponse.success("Danh sách thông báo", notificationRepository
                .findTop50ByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(NotificationResponse::from).toList());
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount(Authentication authentication) {
        return ApiResponse.success("Số thông báo chưa đọc",
                notificationRepository.countByUserIdAndReadAtIsNull(currentUser(authentication).getId()));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markRead(Authentication authentication, @PathVariable UUID id) {
        User user = currentUser(authentication);
        Notification item = notificationRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo"));
        if (item.getReadAt() == null) item.setReadAt(Instant.now());
        return ApiResponse.success("Đã đọc thông báo", NotificationResponse.from(notificationRepository.save(item)));
    }

    @PostMapping("/admin/broadcast")
    public ApiResponse<Integer> broadcast(@Valid @RequestBody BroadcastRequest request) {
        List<Notification> items = userRepository.findAll().stream()
                .filter(user -> Boolean.TRUE.equals(user.getIsActive()))
                .map(user -> create(user, request)).toList();
        notificationRepository.saveAll(items);
        return ApiResponse.success("Đã gửi thông báo", items.size());
    }

    private Notification create(User user, BroadcastRequest request) {
        Notification item = new Notification();
        item.setUser(user);
        item.setTitle(request.title().trim());
        item.setContent(request.content().trim());
        item.setNotificationType("ANNOUNCEMENT");
        item.setActionUrl(request.actionUrl());
        return item;
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }

    public record BroadcastRequest(
            @NotBlank @Size(max = 255) String title,
            @NotBlank @Size(max = 5000) String content,
            @Size(max = 500) String actionUrl
    ) {}
}
