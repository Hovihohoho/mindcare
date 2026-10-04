package com.mindcare.emotionservice.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.UUID;

public class GatewayUserAuthenticationFilter extends OncePerRequestFilter {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLE_HEADER = "X-User-Role";
    public static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final String internalSecret;

    public GatewayUserAuthenticationFilter(AuthenticationEntryPoint authenticationEntryPoint, String internalSecret) {
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.internalSecret = internalSecret;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String headerValue = request.getHeader(USER_ID_HEADER);
        if (headerValue == null || headerValue.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }
        String suppliedSecret = request.getHeader(INTERNAL_SECRET_HEADER);
        if (internalSecret == null || internalSecret.isBlank()
                || suppliedSecret == null || suppliedSecret.isBlank()
                || !MessageDigest.isEqual(internalSecret.getBytes(StandardCharsets.UTF_8),
                        suppliedSecret.getBytes(StandardCharsets.UTF_8))) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response,
                    new InvalidGatewayIdentityException("Trusted gateway hop proof is required"));
            return;
        }
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID userId;
        try {
            userId = UUID.fromString(headerValue.trim());
        } catch (IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new InvalidGatewayIdentityException("X-User-Id must be a valid UUID")
            );
            return;
        }

        UserRole role;
        try {
            role = UserRole.parse(request.getHeader(USER_ROLE_HEADER));
        } catch (IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new InvalidGatewayIdentityException(exception.getMessage())
            );
            return;
        }

        AuthenticatedUser principal = new AuthenticatedUser(userId, role);
        List<SimpleGrantedAuthority> authorities = role == null
                ? List.of()
                : List.of(new SimpleGrantedAuthority(role.name()));
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        filterChain.doFilter(request, response);
    }

    private static final class InvalidGatewayIdentityException
            extends org.springframework.security.core.AuthenticationException {

        private InvalidGatewayIdentityException(String message) {
            super(message);
        }
    }
}
