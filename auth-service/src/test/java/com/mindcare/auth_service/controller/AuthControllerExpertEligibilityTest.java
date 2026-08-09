package com.mindcare.auth_service.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mindcare.auth_service.entity.Role;
import com.mindcare.auth_service.entity.User;
import com.mindcare.auth_service.repository.UserRepository;
import com.mindcare.auth_service.security.JwtUtil;
import com.mindcare.auth_service.service.AccountService;
import com.mindcare.auth_service.service.AuthService;
import com.mindcare.auth_service.service.EmailVerificationService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthControllerExpertEligibilityTest {

    private UserRepository userRepository;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        controller = new AuthController(mock(AuthService.class), mock(AccountService.class),
                mock(EmailVerificationService.class), mock(JwtUtil.class), userRepository);
    }

    @Test
    void bookingProfileRequiresApprovedApplicationInAdditionToExpertRole() {
        User expert = expert("PENDING");
        when(userRepository.findById(expert.getId())).thenReturn(Optional.of(expert));

        AuthController.ExpertBookingProfile pending = controller.bookingProfile(
                expert.getId(), "secret", "secret", new BigDecimal("300000"));
        expert.setExpertStatus("APPROVED");
        AuthController.ExpertBookingProfile approved = controller.bookingProfile(
                expert.getId(), "secret", "secret", new BigDecimal("300000"));

        assertThat(pending.eligible()).isFalse();
        assertThat(approved.eligible()).isTrue();
    }

    @Test
    void internalDirectoryOnlyReturnsApprovedExperts() {
        User pending = expert("PENDING");
        User approved = expert("APPROVED");
        when(userRepository.findByRoleNameAndIsActiveTrueOrderByFullNameAsc("ROLE_EXPERT"))
                .thenReturn(List.of(pending, approved));

        List<AuthController.InternalExpert> result = controller.internalExperts(
                "secret", "secret", new BigDecimal("300000"));

        assertThat(result).extracting(AuthController.InternalExpert::expertUserId)
                .containsExactly(approved.getId());
    }

    private User expert(String status) {
        Role role = new Role();
        role.setName("ROLE_EXPERT");
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFullName("Expert " + status);
        user.setEmail(status.toLowerCase() + "@mindcare.vn");
        user.setRole(role);
        user.setIsActive(true);
        user.setEmailVerified(true);
        user.setExpertStatus(status);
        return user;
    }
}
