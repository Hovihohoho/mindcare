package com.mindcare.auth_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mindcare.auth_service.entity.Notification;
import com.mindcare.auth_service.entity.Role;
import com.mindcare.auth_service.entity.User;
import com.mindcare.auth_service.notification.NotificationSocketHub;
import com.mindcare.auth_service.repository.NotificationRepository;
import com.mindcare.auth_service.repository.RoleRepository;
import com.mindcare.auth_service.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExpertReviewServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationSocketHub notificationSocketHub;
    @Mock private AccountService accountService;
    @Mock private AuditService auditService;
    @InjectMocks private ExpertReviewService service;

    private User applicant;

    @BeforeEach
    void setUp() {
        applicant = new User();
        applicant.setId(UUID.randomUUID());
        applicant.setEmail("applicant@mindcare.vn");
        applicant.setExpertStatus("PENDING");
        when(userRepository.findById(applicant.getId())).thenReturn(Optional.of(applicant));
        lenient().when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void approvalPromotesRoleClearsReasonNotifiesRevokesSessionsAndAudits() {
        when(roleRepository.findByName("ROLE_EXPERT")).thenReturn(Optional.of(role("ROLE_EXPERT")));

        User result = service.reviewPending(
                applicant.getId(), "APPROVED", "ignored", "admin@mindcare.vn");

        assertThat(result.getExpertStatus()).isEqualTo("APPROVED");
        assertThat(result.getRole().getName()).isEqualTo("ROLE_EXPERT");
        assertThat(result.getExpertReviewReason()).isNull();
        assertThat(result.getExpertReviewedAt()).isNotNull();
        verify(accountService).revokeAllSessions(applicant.getId());
        verify(notificationSocketHub).publish(org.mockito.ArgumentMatchers.eq(applicant.getId()), any());
        verify(auditService).record("admin@mindcare.vn", "REVIEW_EXPERT", "USER",
                applicant.getId().toString(), "APPROVED");
    }

    @Test
    void rejectionDemotesRoleAndStoresTrimmedRequiredReason() {
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(role("ROLE_USER")));

        service.reviewPending(applicant.getId(), "REJECTED", "  Thiếu chứng chỉ  ", "admin@mindcare.vn");

        assertThat(applicant.getRole().getName()).isEqualTo("ROLE_USER");
        assertThat(applicant.getExpertStatus()).isEqualTo("REJECTED");
        assertThat(applicant.getExpertReviewReason()).isEqualTo("Thiếu chứng chỉ");
        ArgumentCaptor<Notification> notification = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notification.capture());
        assertThat(notification.getValue().getMessage()).contains("Thiếu chứng chỉ");
        verify(accountService).revokeAllSessions(applicant.getId());
    }

    @Test
    void rejectionRequiresReasonBeforeChangingAnything() {
        assertThatThrownBy(() -> service.reviewPending(
                applicant.getId(), "REJECTED", "   ", "admin@mindcare.vn"))
                .hasMessageContaining("lý do");

        verify(userRepository, never()).save(any());
        verify(notificationRepository, never()).save(any());
        verify(accountService, never()).revokeAllSessions(any());
    }

    @Test
    void reviewRejectsNonPendingApplication() {
        applicant.setExpertStatus("APPROVED");

        assertThatThrownBy(() -> service.reviewPending(
                applicant.getId(), "APPROVED", null, "admin@mindcare.vn"))
                .hasMessageContaining("chờ duyệt");

        verify(userRepository, never()).save(any());
    }

    private Role role(String name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }
}
