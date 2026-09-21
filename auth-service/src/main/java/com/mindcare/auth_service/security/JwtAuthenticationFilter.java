package com.mindcare.auth_service.security;

import com.mindcare.auth_service.entity.User;
import com.mindcare.auth_service.repository.UserRepository;
import com.mindcare.auth_service.repository.UserSessionRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ") &&
                SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String token = header.substring(7);
                User user = userRepository.findByEmail(jwtUtil.extractEmail(token)).orElse(null);
                var session = sessionRepository.findById(jwtUtil.extractSessionId(token)).orElse(null);
                if (user != null && session != null && session.getRevokedAt() == null
                        && session.getExpiresAt().isAfter(java.time.OffsetDateTime.now())
                        && Boolean.TRUE.equals(user.getIsActive()) && jwtUtil.isTokenValid(token)) {
                    session.setLastSeenAt(java.time.OffsetDateTime.now());
                    sessionRepository.save(session);
                    var authority = new SimpleGrantedAuthority(user.getRole().getName());
                    var authentication = new UsernamePasswordAuthenticationToken(
                            user.getEmail(), null, List.of(authority));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException | IllegalArgumentException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
