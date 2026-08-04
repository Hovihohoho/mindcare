package com.mindcare.auth_service.controller;

import com.mindcare.auth_service.dto.ApiResponse;
import com.mindcare.auth_service.dto.NotificationDtos;
import com.mindcare.auth_service.entity.Notification;
import com.mindcare.auth_service.repository.NotificationRepository;
import com.mindcare.auth_service.repository.UserRepository;
import com.mindcare.auth_service.notification.NotificationSocketHub;
import com.mindcare.auth_service.service.CurrentUserService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationRepository repository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUser;
    private final NotificationSocketHub notificationSocketHub;

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
            Notification saved = repository.save(Notification.create(
                    request.userId(),
                    request.eventId(),
                    request.type(),
                    request.title(),
                    request.message(),
                    request.actionUrl()));
            notificationSocketHub.publish(
                    request.userId(),
                    NotificationDtos.Item.from(saved));
        }
        return ApiResponse.success("Đã tiếp nhận thông báo", null);
    }

    @PostMapping("/api/auth/admin/notifications")
    @Transactional
    public ApiResponse<Void> broadcast(@Valid @RequestBody NotificationDtos.Broadcast request) {
        userRepository.findAll().forEach(user -> {
            Notification saved = repository.save(Notification.create(
                    user.getId(),
                    null,
                    "SYSTEM",
                    request.title(),
                    request.message(),
                    request.actionUrl()));
            notificationSocketHub.publish(
                    user.getId(),
                    NotificationDtos.Item.from(saved));
        });
        return ApiResponse.success("Đã gửi thông báo hệ thống", null);
    }
}
