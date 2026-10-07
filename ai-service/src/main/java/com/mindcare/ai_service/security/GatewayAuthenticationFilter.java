package com.mindcare.ai_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class GatewayAuthenticationFilter extends OncePerRequestFilter {
    private final String internalSecret;

    public GatewayAuthenticationFilter(@Value("${app.internal-secret:}") String internalSecret) {
        this.internalSecret = internalSecret;
    }
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final Set<String> ALLOWED_ROLES =
            Set.of("ROLE_USER", "ROLE_ADMIN");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String userIdHeader = request.getHeader(USER_ID_HEADER);
        String role = request.getHeader(USER_ROLE_HEADER);
        String suppliedSecret = request.getHeader("X-Internal-Secret");
        if ((userIdHeader != null || role != null)
                && (internalSecret == null || internalSecret.isBlank()
                    || suppliedSecret == null || suppliedSecret.isBlank()
                    || !MessageDigest.isEqual(internalSecret.getBytes(StandardCharsets.UTF_8),
                            suppliedSecret.getBytes(StandardCharsets.UTF_8)))) {
            SecurityContextHolder.clearContext();
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        if (userIdHeader != null && role != null && ALLOWED_ROLES.contains(role)) {
            try {
                AuthenticatedUser principal =
                        new AuthenticatedUser(UUID.fromString(userIdHeader), role);
                var authentication = UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority(role))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (IllegalArgumentException ignored) {
                SecurityContextHolder.clearContext();
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
