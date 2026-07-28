package com.mindcare.auth_service.service;

import com.mindcare.auth_service.entity.EmailVerificationToken;
import com.mindcare.auth_service.entity.User;
import com.mindcare.auth_service.repository.EmailVerificationTokenRepository;
import com.mindcare.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.frontend-url}") private String frontendUrl;
    @Value("${app.mail-from}") private String mailFrom;
    @Value("${app.verification-expiration-minutes:30}") private long expirationMinutes;

    @Transactional
    public void sendVerification(User user) {
        tokenRepository.deleteByUserId(user.getId());
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES));
        tokenRepository.save(token);

        String link = frontendUrl.replaceAll("/$", "") + "/verify-email?token=" + rawToken;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(user.getEmail());
        message.setSubject("Xác thực tài khoản MindCare");
        message.setText("Xin chào " + user.getFullName() + ",\n\n"
                + "Vui lòng xác thực email bằng liên kết sau (có hiệu lực " + expirationMinutes + " phút):\n"
                + link + "\n\nNếu bạn không đăng ký MindCare, hãy bỏ qua email này.");
        mailSender.send(message);
    }

    @Transactional
    public void verify(String rawToken) {
        EmailVerificationToken token = tokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new RuntimeException("Liên kết xác thực không hợp lệ"));
        if (token.getUsedAt() != null && Boolean.TRUE.equals(token.getUser().getEmailVerified())) {
            return;
        }
        if (token.getUsedAt() != null) throw new RuntimeException("Liên kết xác thực đã được sử dụng");
        if (token.getExpiresAt().isBefore(Instant.now())) throw new RuntimeException("Liên kết xác thực đã hết hạn");
        User user = token.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        token.setUsedAt(Instant.now());
        tokenRepository.save(token);
    }

    @Transactional
    public void resend(String email) {
        userRepository.findByEmail(email.trim().toLowerCase())
                .filter(user -> !Boolean.TRUE.equals(user.getEmailVerified()))
                .ifPresent(this::sendVerification);
    }

    private String hash(String value) {
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
