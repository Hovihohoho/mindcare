package com.mindcare.ai_service.security;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final GatewayAuthenticationFilter gatewayAuthenticationFilter;

    public SecurityConfig(GatewayAuthenticationFilter gatewayAuthenticationFilter) {
        this.gatewayAuthenticationFilter = gatewayAuthenticationFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/ws/ai/**").authenticated()
                        .requestMatchers("/api/ai/documents/**").hasRole("ADMIN")
                        .requestMatchers("/api/ai/self-care-content/**").hasRole("USER")
                        .requestMatchers("/api/ai/chat/**").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) ->
                                writeError(response, 401, "AUTHENTICATION_REQUIRED",
                                        "Bạn cần đăng nhập để sử dụng chức năng này."))
                        .accessDeniedHandler((request, response, exception) ->
                                writeError(response, 403, "ACCESS_DENIED",
                                        "Bạn không có quyền thực hiện chức năng này.")))
                .addFilterBefore(
                        gatewayAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private void writeError(
            HttpServletResponse response,
            int status,
            String code,
            String message
    ) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"success":false,"code":"%s","message":"%s","timestamp":"%s"}
                """.formatted(code, message, Instant.now()));
    }
}
