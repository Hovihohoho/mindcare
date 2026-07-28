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

@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.mail-from}") private String mailFrom;
    @Value("${app.verification-expiration-minutes:10}") private long expirationMinutes;

    @Transactional
    public void sendVerification(User user) {
        tokenRepository.deleteByUserId(user.getId());
        String code = "%06d".formatted(secureRandom.nextInt(1_000_000));
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setTokenHash(hash(code));
        token.setExpiresAt(Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES));
        tokenRepository.save(token);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(user.getEmail());
        message.setSubject("Mã xác thực tài khoản MindCare");
        message.setText("Xin chào " + user.getFullName() + ",\n\n"
                + "Mã xác thực tài khoản MindCare của bạn là:\n\n"
                + code + "\n\n"
                + "Mã có hiệu lực trong " + expirationMinutes + " phút. "
                + "Không chia sẻ mã này với bất kỳ ai.\n\n"
                + "Nếu bạn không đăng ký MindCare, hãy bỏ qua email này.");
        mailSender.send(message);
    }

    @Transactional
    public void verifyCode(String email, String code) {
        if (email == null || code == null || !code.matches("\\d{6}")) {
            throw new RuntimeException("Email hoặc mã xác thực không hợp lệ");
        }
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new RuntimeException("Mã xác thực không hợp lệ"));
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return;
        }
        EmailVerificationToken token = tokenRepository.findByUserIdAndTokenHash(user.getId(), hash(code))
                .orElseThrow(() -> new RuntimeException("Mã xác thực không đúng"));
        if (token.getUsedAt() != null) {
            throw new RuntimeException("Mã xác thực đã được sử dụng");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Mã xác thực đã hết hạn");
        }
        user.setEmailVerified(true);
        userRepository.save(user);
        token.setUsedAt(Instant.now());
        tokenRepository.save(token);
    }

    @Transactional
    public void resend(String email) {
        if (email == null || email.isBlank()) return;
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
