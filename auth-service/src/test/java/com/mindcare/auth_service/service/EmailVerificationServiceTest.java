package com.mindcare.auth_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mindcare.auth_service.entity.EmailVerificationToken;
import com.mindcare.auth_service.entity.User;
import com.mindcare.auth_service.repository.EmailVerificationTokenRepository;
import com.mindcare.auth_service.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock private EmailVerificationTokenRepository tokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private JavaMailSender mailSender;
    @InjectMocks private EmailVerificationService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@mindcare.vn");
        user.setEmailVerified(false);
    }

    @Test
    void verifyNormalizesEmailAndConsumesTokenOnce() {
        EmailVerificationToken token = token("123456", Instant.now().plusSeconds(60));
        when(tokenRepository.findByTokenHash(hash("user@mindcare.vn:123456")))
                .thenReturn(Optional.of(token));

        service.verify("  USER@MINDCARE.VN ", "123456");

        assertThat(user.getEmailVerified()).isTrue();
        assertThat(token.getUsedAt()).isNotNull();
        verify(userRepository).save(user);
        verify(tokenRepository).save(token);
    }

    @Test
    void verifyRejectsExpiredTokenWithoutChangingUser() {
        EmailVerificationToken token = token("123456", Instant.now().minusSeconds(1));
        when(tokenRepository.findByTokenHash(hash("user@mindcare.vn:123456")))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.verify(user.getEmail(), "123456"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("hết hạn");
        assertThat(user.getEmailVerified()).isFalse();
        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyIsIdempotentAfterSuccessfulVerification() {
        EmailVerificationToken token = token("123456", Instant.now().plusSeconds(60));
        token.setUsedAt(Instant.now());
        user.setEmailVerified(true);
        when(tokenRepository.findByTokenHash(hash("user@mindcare.vn:123456")))
                .thenReturn(Optional.of(token));

        service.verify(user.getEmail(), "123456");

        verify(userRepository, never()).save(any());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void verifyRejectsMalformedCodeBeforeRepositoryLookup() {
        assertThatThrownBy(() -> service.verify(user.getEmail(), "12A"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("6 chữ số");
        verify(tokenRepository, never()).findByTokenHash(any());
    }

    private EmailVerificationToken token(String code, Instant expiresAt) {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setTokenHash(hash(user.getEmail() + ":" + code));
        token.setExpiresAt(expiresAt);
        return token;
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
