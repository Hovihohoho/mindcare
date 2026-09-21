package com.mindcare.auth_service.controller;

import com.mindcare.auth_service.dto.ApiResponse;
import com.mindcare.auth_service.dto.NotificationDtos;
import com.mindcare.auth_service.entity.Notification;
import com.mindcare.auth_service.notification.NotificationSocketHub;
import com.mindcare.auth_service.repository.NotificationRepository;
import com.mindcare.auth_service.repository.UserRepository;
import com.mindcare.auth_service.service.AccountService;
import com.mindcare.auth_service.service.AuditService;
import com.mindcare.auth_service.service.CurrentUserService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationRepository repository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUser;
    private final AccountService accountService;
    private final AuditService auditService;
    private final NotificationSocketHub notificationSocketHub;

    @GetMapping("/api/auth/notifications")
    public ApiResponse<List<LegacyResponse>> legacyList(Authentication auth) {
        UUID userId = accountService.current(auth.getName()).getId();
        return ApiResponse.success("Thông báo", repository.findTop50ByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(LegacyResponse::from).toList());
    }

    @GetMapping("/api/auth/notifications/unread-count")
    public ApiResponse<Long> legacyUnread(Authentication auth) {
        return ApiResponse.success("Số thông báo chưa đọc",
                repository.countByUserIdAndReadAtIsNull(accountService.current(auth.getName()).getId()));
    }

    @PatchMapping("/api/auth/notifications/{id}/read")
    @Transactional
    public ApiResponse<LegacyResponse> legacyRead(Authentication auth, @PathVariable UUID id) {
        UUID userId = accountService.current(auth.getName()).getId();
        Notification item = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        item.markRead();
        return ApiResponse.success("Đã đọc", LegacyResponse.from(item));
    }

    @PatchMapping("/api/auth/notifications/read-all")
    @Transactional
    public ApiResponse<Integer> legacyReadAll(Authentication auth) {
        int count = repository.markAllRead(accountService.current(auth.getName()).getId());
        return ApiResponse.success("Đã đánh dấu tất cả là đã đọc", count);
    }

    @PostMapping("/api/auth/notifications/admin/broadcast")
    public ApiResponse<Integer> legacyBroadcast(Authentication auth, @Valid @RequestBody LegacyBroadcast request) {
        var users = userRepository.findAll().stream().filter(user -> Boolean.TRUE.equals(user.getIsActive())).toList();
        users.forEach(user -> publish(repository.save(Notification.create(
                user.getId(), null, "ANNOUNCEMENT", request.title(), request.content(), request.actionUrl()))));
        auditService.record(auth.getName(), "BROADCAST_NOTIFICATION", "NOTIFICATION", null,
                request.title() + " (" + users.size() + " users)");
        return ApiResponse.success("Đã gửi thông báo", users.size());
    }

    @GetMapping("/api/v1/notifications")
    public ApiResponse<NotificationDtos.Page> list(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "10") int size) {
        UUID userId = currentUser.id();
        int safeSize = Math.max(1, Math.min(size, 50));
        var values = repository.findByUserIdOrderByCreatedAtDescIdDesc(
                userId, PageRequest.of(Math.max(0, page), safeSize));
        return ApiResponse.success("Danh sách thông báo",
                new NotificationDtos.Page(values.getContent().stream().map(NotificationDtos.Item::from).toList(),
                        values.getNumber(), values.getSize(), values.getTotalElements(),
                        values.getTotalPages(), repository.countByUserIdAndReadAtIsNull(userId)));
    }

    @PatchMapping("/api/v1/notifications/{id}/read")
    @Transactional
    public ApiResponse<Void> markRead(@PathVariable UUID id) {
        repository.findByIdAndUserId(id, currentUser.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)).markRead();
        return ApiResponse.success("Đã đánh dấu đã đọc", null);
    }

    @PatchMapping("/api/v1/notifications/read-all")
    @Transactional
    public ApiResponse<Void> markAllRead() {
        repository.markAllRead(currentUser.id());
        return ApiResponse.success("Đã đọc tất cả thông báo", null);
    }

    @PostMapping("/api/auth/internal/notifications")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<Void> receive(@RequestHeader("X-Internal-Secret") String suppliedSecret,
                                     @Value("${app.internal-secret}") String expectedSecret,
                                     @Valid @RequestBody NotificationDtos.InternalCreate request) {
        if (!expectedSecret.equals(suppliedSecret)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if (!userRepository.existsById(request.userId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        if (!repository.existsBySourceEventIdAndUserId(request.eventId(), request.userId())) {
            publish(repository.save(Notification.create(request.userId(), request.eventId(), request.type(),
                    request.title(), request.message(), request.actionUrl())));
        }
        return ApiResponse.success("Đã tiếp nhận thông báo", null);
    }

    @PostMapping("/api/auth/admin/notifications")
    @Transactional
    public ApiResponse<Void> broadcast(@Valid @RequestBody NotificationDtos.Broadcast request) {
        userRepository.findAll().forEach(user -> publish(repository.save(Notification.create(
                user.getId(), null, "SYSTEM", request.title(), request.message(), request.actionUrl()))));
        return ApiResponse.success("Đã gửi thông báo hệ thống", null);
    }

    private void publish(Notification notification) {
        notificationSocketHub.publish(notification.getUserId(), NotificationDtos.Item.from(notification));
    }

    public record LegacyBroadcast(@NotBlank @Size(max = 160) String title,
                                  @NotBlank @Size(max = 500) String content,
                                  @Size(max = 500) String actionUrl) {}
    public record LegacyResponse(UUID id, String title, String content, String notificationType,
                                 String actionUrl, OffsetDateTime readAt, OffsetDateTime createdAt) {
        static LegacyResponse from(Notification item) {
            return new LegacyResponse(item.getId(), item.getTitle(), item.getMessage(), item.getType(),
                    item.getActionUrl(), item.getReadAt(), item.getCreatedAt());
        }
    }
}
