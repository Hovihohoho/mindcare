package com.mindcare.auth_service.controller;

import com.mindcare.auth_service.dto.ApiResponse;
import com.mindcare.auth_service.entity.Notification;
import com.mindcare.auth_service.entity.User;
import com.mindcare.auth_service.repository.NotificationRepository;
import com.mindcare.auth_service.repository.UserRepository;
import com.mindcare.auth_service.service.AccountService;
import com.mindcare.auth_service.service.AuditService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationRepository repository;
    private final UserRepository userRepository;
    private final AccountService accountService;
    private final AuditService auditService;

    @GetMapping
    public ApiResponse<List<Response>> list(Authentication auth) {
        User user = accountService.current(auth.getName());
        return ApiResponse.success("Thông báo", repository.findTop50ByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().map(Response::from).toList());
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unread(Authentication auth) {
        return ApiResponse.success("Số thông báo chưa đọc",
                repository.countByUserIdAndReadAtIsNull(accountService.current(auth.getName()).getId()));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<Response> read(Authentication auth, @PathVariable UUID id) {
        User user = accountService.current(auth.getName());
        Notification item = repository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo"));
        item.setReadAt(OffsetDateTime.now());
        return ApiResponse.success("Đã đọc", Response.from(repository.save(item)));
    }

    @PatchMapping("/read-all")
    public ApiResponse<Integer> readAll(Authentication auth) {
        User user = accountService.current(auth.getName());
        List<Notification> unread = repository.findTop50ByUserIdOrderByCreatedAtDesc(user.getId())
                .stream().filter(item -> item.getReadAt() == null).toList();
        OffsetDateTime now = OffsetDateTime.now();
        unread.forEach(item -> item.setReadAt(now));
        repository.saveAll(unread);
        return ApiResponse.success("Đã đánh dấu tất cả là đã đọc", unread.size());
    }

    @PostMapping("/admin/broadcast")
    public ApiResponse<Integer> broadcast(Authentication auth, @Valid @RequestBody Broadcast request) {
        List<Notification> items = userRepository.findAll().stream()
                .filter(user -> Boolean.TRUE.equals(user.getIsActive()))
                .map(user -> {
                    Notification item = new Notification();
                    item.setUser(user);
                    item.setTitle(request.title());
                    item.setContent(request.content());
                    item.setActionUrl(request.actionUrl());
                    item.setNotificationType("ANNOUNCEMENT");
                    return item;
                }).toList();
        repository.saveAll(items);
        auditService.record(auth.getName(), "BROADCAST_NOTIFICATION", "NOTIFICATION", null,
                request.title() + " (" + items.size() + " users)");
        return ApiResponse.success("Đã gửi thông báo", items.size());
    }

    public record Broadcast(@NotBlank @Size(max = 255) String title,
                            @NotBlank @Size(max = 5000) String content,
                            @Size(max = 500) String actionUrl) {}
    public record Response(UUID id, String title, String content, String notificationType,
                           String actionUrl, OffsetDateTime readAt, OffsetDateTime createdAt) {
        static Response from(Notification item) {
            return new Response(item.getId(), item.getTitle(), item.getContent(),
                    item.getNotificationType(), item.getActionUrl(), item.getReadAt(), item.getCreatedAt());
        }
    }
}
