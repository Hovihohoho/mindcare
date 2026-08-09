package com.mindcare.auth_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mindcare.auth_service.dto.AccountRequests;
import com.mindcare.auth_service.entity.PasswordResetToken;
import com.mindcare.auth_service.entity.User;
import com.mindcare.auth_service.entity.UserSession;
import com.mindcare.auth_service.repository.PasswordResetTokenRepository;
import com.mindcare.auth_service.repository.UserRepository;
import com.mindcare.auth_service.repository.UserSessionRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AccountServiceRecoveryTest {

    @Mock private UserRepository userRepository;
    @Mock private UserSessionRepository sessionRepository;
    @Mock private PasswordResetTokenRepository resetRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JavaMailSender mailSender;
    @Mock private PasswordPolicy passwordPolicy;
    @InjectMocks private AccountService service;

    private User user;
    private PasswordResetToken token;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setPassword("old-hash");
        token = new PasswordResetToken();
        token.setUser(user);
        token.setTokenHash(hash("raw-token"));
        token.setExpiresAt(OffsetDateTime.now().plusMinutes(5));
    }

    @Test
    void resetPasswordConsumesTokenAndRevokesEverySession() {
        UserSession first = session();
        UserSession second = session();
        when(resetRepository.findByTokenHash(hash("raw-token"))).thenReturn(Optional.of(token));
        when(passwordEncoder.matches("NewStrong1", user.getPassword())).thenReturn(false);
        when(passwordEncoder.encode("NewStrong1")).thenReturn("new-hash");
        when(sessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId()))
                .thenReturn(List.of(first, second));

        service.resetPassword(new AccountRequests.ResetPassword("raw-token", "NewStrong1"));

        assertThat(user.getPassword()).isEqualTo("new-hash");
        assertThat(token.getUsedAt()).isNotNull();
        assertThat(first.getRevokedAt()).isNotNull();
        assertThat(second.getRevokedAt()).isNotNull();
        verify(userRepository).save(user);
        verify(resetRepository).save(token);
    }

    @Test
    void resetPasswordRejectsExpiredToken() {
        token.setExpiresAt(OffsetDateTime.now().minusSeconds(1));
        when(resetRepository.findByTokenHash(hash("raw-token"))).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.resetPassword(
                new AccountRequests.ResetPassword("raw-token", "NewStrong1")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("hết hạn");
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPasswordRejectsReusingCurrentPassword() {
        when(resetRepository.findByTokenHash(hash("raw-token"))).thenReturn(Optional.of(token));
        when(passwordEncoder.matches("SameStrong1", user.getPassword())).thenReturn(true);

        assertThatThrownBy(() -> service.resetPassword(
                new AccountRequests.ResetPassword("raw-token", "SameStrong1")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("phải khác");
        assertThat(token.getUsedAt()).isNull();
        verify(userRepository, never()).save(any());
    }

    private UserSession session() {
        UserSession session = new UserSession();
        session.setId(UUID.randomUUID());
        session.setUser(user);
        return session;
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
