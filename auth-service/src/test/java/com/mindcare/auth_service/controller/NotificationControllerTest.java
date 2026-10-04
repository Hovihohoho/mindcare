package com.mindcare.auth_service.controller;

import com.mindcare.auth_service.dto.NotificationDtos;
import com.mindcare.auth_service.entity.Notification;
import com.mindcare.auth_service.notification.NotificationDeliveryService;
import com.mindcare.auth_service.repository.NotificationRepository;
import com.mindcare.auth_service.repository.UserRepository;
import com.mindcare.auth_service.service.AccountService;
import com.mindcare.auth_service.service.AuditService;
import com.mindcare.auth_service.service.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {
    @Mock private NotificationRepository repository;
    @Mock private UserRepository userRepository;
    @Mock private CurrentUserService currentUser;
    @Mock private AccountService accountService;
    @Mock private AuditService auditService;
    @Mock private NotificationDeliveryService delivery;

    @Test
    void internalNotificationPersistsAndQueuesPushDelivery() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(userRepository.existsById(userId)).thenReturn(true);
        when(repository.existsBySourceEventIdAndUserId(eventId, userId)).thenReturn(false);
        when(repository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        NotificationController controller = new NotificationController(
                repository, userRepository, currentUser, accountService, auditService, delivery);

        controller.receive("secret", "secret", new NotificationDtos.InternalCreate(
                eventId, userId, "HEALTH_BENCHMARK", "Giấc ngủ cần được lưu ý",
                "Thời lượng ngủ dưới benchmark.", "/health-connect"));

        verify(delivery).enqueue(org.mockito.ArgumentMatchers.argThat(item ->
                item.getUserId().equals(userId) && item.getSourceEventId().equals(eventId)
                        && item.getType().equals("HEALTH_BENCHMARK")
                        && item.getActionUrl().equals("/health-connect")), eq(true));
    }
}
