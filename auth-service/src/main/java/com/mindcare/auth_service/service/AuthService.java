package com.mindcare.auth_service.service;

import com.mindcare.auth_service.dto.AuthRequest;
import com.mindcare.auth_service.dto.AuthResponse;
import com.mindcare.auth_service.dto.UserSummary;
import com.mindcare.auth_service.entity.Role;
import com.mindcare.auth_service.entity.User;
import com.mindcare.auth_service.repository.RoleRepository;
import com.mindcare.auth_service.repository.UserRepository;
import com.mindcare.auth_service.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailVerificationService emailVerificationService;

    public AuthResponse login(AuthRequest.Login request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new RuntimeException("Email hoặc mật khẩu không đúng"));
        if (!Boolean.TRUE.equals(user.getIsActive()) ||
                !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Email hoặc mật khẩu không đúng");
        }
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new RuntimeException("Email chưa được xác thực. Vui lòng kiểm tra hộp thư");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().getName());
        return AuthResponse.bearer(token, jwtUtil.getExpirationSeconds(), UserSummary.from(user));
    }

    @Transactional
    public void register(AuthRequest.Register request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email đã được sử dụng");
        }
        Role role = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ROLE_USER"));
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setEmailVerified(false);
        userRepository.save(user);
        emailVerificationService.sendVerification(user);
    }
}
