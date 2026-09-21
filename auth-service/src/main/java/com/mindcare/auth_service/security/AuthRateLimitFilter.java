package com.mindcare.auth_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private static final Map<String, Rule> RULES = Map.of(
            "/api/auth/login", new Rule(10, Duration.ofMinutes(5)),
            "/api/auth/register", new Rule(5, Duration.ofHours(1)),
            "/api/auth/verify-email", new Rule(10, Duration.ofMinutes(15)),
            "/api/auth/forgot-password", new Rule(5, Duration.ofMinutes(15)),
            "/api/auth/resend-verification", new Rule(5, Duration.ofMinutes(15))
    );
    private final Map<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod()) || !RULES.containsKey(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        Rule rule = RULES.get(path);
        String key = path + ":" + clientIp(request);
        Deque<Instant> timestamps = attempts.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        Instant now = Instant.now();
        boolean allowed;
        long retryAfter;
        synchronized (timestamps) {
            Instant threshold = now.minus(rule.window());
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(threshold)) {
                timestamps.removeFirst();
            }
            allowed = timestamps.size() < rule.limit();
            if (allowed) {
                timestamps.addLast(now);
                retryAfter = 0;
            } else {
                retryAfter = Math.max(1, Duration.between(now,
                        timestamps.peekFirst().plus(rule.window())).toSeconds());
            }
        }
        if (!allowed) {
            response.setStatus(429);
            response.setHeader("Retry-After", Long.toString(retryAfter));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Bạn thao tác quá nhanh. Vui lòng thử lại sau.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    private record Rule(int limit, Duration window) {}
}
