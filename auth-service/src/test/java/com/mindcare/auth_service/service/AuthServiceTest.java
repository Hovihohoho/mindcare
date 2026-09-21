package com.mindcare.auth_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mindcare.auth_service.dto.AuthRequest;
import com.mindcare.auth_service.entity.Role;
import com.mindcare.auth_service.entity.User;
import com.mindcare.auth_service.entity.UserSession;
import com.mindcare.auth_service.repository.RoleRepository;
import com.mindcare.auth_service.repository.UserRepository;
import com.mindcare.auth_service.repository.UserSessionRepository;
import com.mindcare.auth_service.security.JwtUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private EmailVerificationService emailVerificationService;
    @Mock private UserSessionRepository sessionRepository;
    @Mock private PasswordPolicy passwordPolicy;
    @InjectMocks private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        Role role = new Role();
        role.setName("ROLE_USER");
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@mindcare.vn");
        user.setFullName("MindCare User");
        user.setPassword("encoded-password");
        user.setRole(role);
        user.setIsActive(true);
        user.setEmailVerified(true);
    }

    @Test
    void loginNormalizesEmailCreatesSessionAndReturnsToken() {
        AuthRequest.Login request = login("  USER@MINDCARE.VN ", "StrongPass1");
        when(userRepository.findByEmail("user@mindcare.vn")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("StrongPass1", user.getPassword())).thenReturn(true);
        when(jwtUtil.getExpirationSeconds()).thenReturn(3600L);
        when(sessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of());
        when(jwtUtil.generateToken(any(), any(), any(), any())).thenReturn("signed-token");

        var response = authService.login(request, "browser", "127.0.0.1");

        assertThat(response.accessToken()).isEqualTo("signed-token");
        assertThat(response.user().id()).isEqualTo(user.getId());
        ArgumentCaptor<UserSession> session = ArgumentCaptor.forClass(UserSession.class);
        verify(sessionRepository).save(session.capture());
        assertThat(session.getValue().getUserAgent()).isEqualTo("browser");
        assertThat(session.getValue().getIpAddress()).isEqualTo("127.0.0.1");
        assertThat(session.getValue().getExpiresAt()).isAfter(session.getValue().getCreatedAt());
    }

    @Test
    void loginRejectsInactiveAccountWithoutCreatingSession() {
        user.setIsActive(false);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(login(user.getEmail(), "StrongPass1"), null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("không đúng");
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void loginRejectsUnverifiedEmailWithoutCreatingSession() {
        user.setEmailVerified(false);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("StrongPass1", user.getPassword())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(login(user.getEmail(), "StrongPass1"), null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("chưa được xác thực");
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void loginRevokesSessionsBeyondTenActiveSessions() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("StrongPass1", user.getPassword())).thenReturn(true);
        when(jwtUtil.getExpirationSeconds()).thenReturn(3600L);
        when(jwtUtil.generateToken(any(), any(), any(), any())).thenReturn("signed-token");
        List<UserSession> sessions = new ArrayList<>();
        for (int index = 0; index < 11; index++) sessions.add(new UserSession());
        when(sessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(sessions);

        authService.login(login(user.getEmail(), "StrongPass1"), null, null);

        assertThat(sessions.get(9).getRevokedAt()).isNull();
        assertThat(sessions.get(10).getRevokedAt()).isNotNull();
    }

    @Test
    void registerNormalizesEmailAndStartsVerification() {
        AuthRequest.Register request = new AuthRequest.Register();
        request.setFullName("New User");
        request.setEmail("  NEW@MINDCARE.VN ");
        request.setPassword("StrongPass1");
        Role role = user.getRole();
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded");

        authService.register(request);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("new@mindcare.vn");
        assertThat(saved.getValue().getEmailVerified()).isFalse();
        verify(emailVerificationService).sendVerification(saved.getValue());
    }

    private AuthRequest.Login login(String email, String password) {
        AuthRequest.Login request = new AuthRequest.Login();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }
}
