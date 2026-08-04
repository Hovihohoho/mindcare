package com.mindcare.auth_service.service;

import com.mindcare.auth_service.dto.AccountRequests;
import com.mindcare.auth_service.dto.UserSummary;
import com.mindcare.auth_service.entity.PasswordResetToken;
import com.mindcare.auth_service.entity.User;
import com.mindcare.auth_service.entity.UserSession;
import com.mindcare.auth_service.repository.PasswordResetTokenRepository;
import com.mindcare.auth_service.repository.UserRepository;
import com.mindcare.auth_service.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.MailSendException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final PasswordResetTokenRepository resetRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final PasswordPolicy passwordPolicy;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.frontend-url}") private String frontendUrl;
    @Value("${app.mail-from}") private String mailFrom;

    public User current(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không còn hợp lệ"));
    }

    @Transactional
    public UserSummary updateProfile(String email, AccountRequests.UpdateProfile request) {
        User user = current(email);
        user.setFullName(request.fullName().trim());
        user.setPhone(blankToNull(request.phone()));
        user.setBirthDate(request.birthDate());
        user.setGender(blankToNull(request.gender()));
        user.setAddress(blankToNull(request.address()));
        user.setBio(blankToNull(request.bio()));
        if ("ROLE_EXPERT".equals(user.getRole().getName()) || !"NONE".equals(user.getExpertStatus())) {
            user.setHeadline(blankToNull(request.headline()));
            user.setSpecialties(blankToNull(request.specialties()));
            user.setYearsOfExperience(request.yearsOfExperience());
            user.setConsultationFee(request.consultationFee());
            user.setWorkplace(blankToNull(request.workplace()));
            user.setEducation(blankToNull(request.education()));
        }
        return UserSummary.from(userRepository.save(user));
    }

    @Transactional
    public void changePassword(String email, AccountRequests.ChangePassword request) {
        User user = current(email);
        passwordPolicy.validate(request.newPassword());
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu hiện tại không đúng");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu mới phải khác mật khẩu hiện tại");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        revokeAll(user.getId(), null);
    }

    @Transactional
    public void deactivate(String email) {
        User user = current(email);
        user.setIsActive(false);
        user.setDeletedAt(OffsetDateTime.now());
        userRepository.save(user);
        revokeAll(user.getId(), null);
    }

    public List<UserSession> sessions(String email) {
        return sessionRepository.findByUserIdOrderByCreatedAtDesc(current(email).getId());
    }

    @Transactional
    public void revokeSession(String email, UUID sessionId) {
        User user = current(email);
        UserSession session = sessionRepository.findById(sessionId)
                .filter(item -> item.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên đăng nhập"));
        session.setRevokedAt(OffsetDateTime.now());
        sessionRepository.save(session);
    }

    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(email.trim().toLowerCase())
                .filter(user -> Boolean.TRUE.equals(user.getIsActive()))
                .ifPresent(user -> {
            resetRepository.deleteByUserId(user.getId());
            byte[] bytes = new byte[32];
            random.nextBytes(bytes);
            String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            PasswordResetToken token = new PasswordResetToken();
            token.setUser(user);
            token.setTokenHash(hash(raw));
            token.setExpiresAt(OffsetDateTime.now().plusMinutes(30));
            resetRepository.save(token);
            String resetUrl = frontendUrl.replaceAll("/$", "") + "/reset-password?token="
                    + URLEncoder.encode(raw, StandardCharsets.UTF_8);
            try {
                var message = mailSender.createMimeMessage();
                // Multipart is required when providing both plain-text and HTML alternatives.
                var helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
                helper.setFrom(mailFrom);
                helper.setTo(user.getEmail());
                helper.setSubject("Đặt lại mật khẩu MindCare");
                helper.setText(
                        "Mở liên kết sau trên máy tính đang chạy MindCare trong vòng 30 phút:\n" + resetUrl,
                        "<div style=\"font-family:Arial,sans-serif;line-height:1.6;color:#172033\">"
                                + "<h2>Đặt lại mật khẩu MindCare</h2>"
                                + "<p>Liên kết này có hiệu lực trong 30 phút và chỉ sử dụng được một lần.</p>"
                                + "<p><a href=\"" + resetUrl + "\" style=\"display:inline-block;padding:12px 20px;"
                                + "background:#00658d;color:#fff;text-decoration:none;border-radius:8px\">"
                                + "Đặt lại mật khẩu</a></p>"
                                + "<p>Nếu nút không mở được, hãy sao chép địa chỉ sau vào trình duyệt trên máy tính đang chạy MindCare:</p>"
                                + "<p style=\"word-break:break-all\">" + resetUrl + "</p>"
                                + "<p>Nếu bạn không yêu cầu thao tác này, hãy bỏ qua email.</p></div>");
                mailSender.send(message);
            } catch (jakarta.mail.MessagingException exception) {
                throw new MailSendException("Không thể tạo email đặt lại mật khẩu", exception);
            }
        });
    }

    @Transactional
    public void resetPassword(AccountRequests.ResetPassword request) {
        passwordPolicy.validate(request.newPassword());
        PasswordResetToken token = resetRepository.findByTokenHash(hash(request.token()))
                .orElseThrow(() -> new RuntimeException("Liên kết đặt lại mật khẩu không hợp lệ"));
        if (token.getUsedAt() != null || token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new RuntimeException("Liên kết đặt lại mật khẩu đã hết hạn hoặc được sử dụng");
        }
        if (passwordEncoder.matches(request.newPassword(), token.getUser().getPassword())) {
            throw new RuntimeException("Mật khẩu mới phải khác mật khẩu hiện tại");
        }
        token.getUser().setPassword(passwordEncoder.encode(request.newPassword()));
        token.setUsedAt(OffsetDateTime.now());
        userRepository.save(token.getUser());
        resetRepository.save(token);
        revokeAll(token.getUser().getId(), null);
    }

    public void setAvatar(String email, String url) {
        User user = current(email);
        user.setAvatarUrl(url);
        userRepository.save(user);
    }

    @Transactional
    public void revokeAllSessions(UUID userId) {
        revokeAll(userId, null);
    }

    private void revokeAll(UUID userId, UUID except) {
        sessionRepository.findByUserIdOrderByCreatedAtDesc(userId).forEach(session -> {
            if (except == null || !session.getId().equals(except)) {
                session.setRevokedAt(OffsetDateTime.now());
                sessionRepository.save(session);
            }
        });
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
