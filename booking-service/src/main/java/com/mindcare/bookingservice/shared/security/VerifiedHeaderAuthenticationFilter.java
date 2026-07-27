package com.mindcare.bookingservice.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class VerifiedHeaderAuthenticationFilter extends OncePerRequestFilter {

    static final String USER_ID_HEADER = "X-User-Id";
    static final String USER_ROLE_HEADER = "X-User-Role";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String userIdValue = request.getHeader(USER_ID_HEADER);
        String roleValue = request.getHeader(USER_ROLE_HEADER);

        if (userIdValue != null || roleValue != null) {
            try {
                UUID userId = UUID.fromString(userIdValue);
                UserRole role = UserRole.valueOf(roleValue);
                AuthenticatedUser principal = new AuthenticatedUser(userId, role);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority(role.name())));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (RuntimeException invalidHeader) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setHeader(HttpHeaders.CONTENT_TYPE, "application/problem+json");
                response.getWriter().write(
                        "{\"status\":401,\"code\":\"INVALID_VERIFIED_IDENTITY\","
                                + "\"detail\":\"Verified identity headers are invalid\"}");
                return;
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
