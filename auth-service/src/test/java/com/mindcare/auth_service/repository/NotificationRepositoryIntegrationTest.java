package com.mindcare.auth_service.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mindcare.auth_service.entity.Notification;
import com.mindcare.auth_service.entity.Role;
import com.mindcare.auth_service.entity.User;
import com.mindcare.auth_service.support.AbstractPostgreSqlIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class NotificationRepositoryIntegrationTest extends AbstractPostgreSqlIntegrationTest {

    @Autowired private NotificationRepository notificationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;

    private User firstUser;
    private User secondUser;

    @BeforeEach
    void setUp() {
        Role role = roleRepository.findByName("ROLE_USER").orElseThrow();
        firstUser = saveUser("notification-one-" + UUID.randomUUID() + "@mindcare.test", role);
        secondUser = saveUser("notification-two-" + UUID.randomUUID() + "@mindcare.test", role);
    }

    @Test
    void queriesAndReadOperationsNeverCrossUserBoundary() {
        Notification first = notificationRepository.save(notification(firstUser.getId(), UUID.randomUUID()));
        notificationRepository.save(notification(firstUser.getId(), UUID.randomUUID()));
        notificationRepository.save(notification(secondUser.getId(), UUID.randomUUID()));

        assertThat(notificationRepository.findByIdAndUserId(first.getId(), firstUser.getId())).isPresent();
        assertThat(notificationRepository.findByIdAndUserId(first.getId(), secondUser.getId())).isEmpty();
        assertThat(notificationRepository.countByUserIdAndReadAtIsNull(firstUser.getId())).isEqualTo(2);
        assertThat(notificationRepository.countByUserIdAndReadAtIsNull(secondUser.getId())).isOne();

        first.markRead();
        notificationRepository.flush();
        assertThat(notificationRepository.countByUserIdAndReadAtIsNull(firstUser.getId())).isOne();

        assertThat(notificationRepository.markAllRead(firstUser.getId())).isOne();
        notificationRepository.flush();
        assertThat(notificationRepository.countByUserIdAndReadAtIsNull(firstUser.getId())).isZero();
        assertThat(notificationRepository.countByUserIdAndReadAtIsNull(secondUser.getId())).isOne();
    }

    @Test
    void sourceEventIsIdempotentPerUserButReusableForAnotherUser() {
        UUID eventId = UUID.randomUUID();
        notificationRepository.saveAndFlush(notification(firstUser.getId(), eventId));
        notificationRepository.saveAndFlush(notification(secondUser.getId(), eventId));

        assertThat(notificationRepository.existsBySourceEventIdAndUserId(eventId, firstUser.getId())).isTrue();
        assertThat(notificationRepository.existsBySourceEventIdAndUserId(eventId, secondUser.getId())).isTrue();
        assertThatThrownBy(() -> notificationRepository.saveAndFlush(notification(firstUser.getId(), eventId)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private User saveUser(String email, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("not-a-real-password-hash");
        user.setFullName("Notification Test User");
        user.setRole(role);
        user.setEmailVerified(true);
        return userRepository.saveAndFlush(user);
    }

    private Notification notification(UUID userId, UUID eventId) {
        return Notification.create(userId, eventId, "BOOKING_REMINDER", "Nhắc lịch",
                "Bạn có một lịch tư vấn sắp tới.", "/bookings");
    }
}
