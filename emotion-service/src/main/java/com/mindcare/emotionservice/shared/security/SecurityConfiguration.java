package com.mindcare.emotionservice.shared.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindcare.emotionservice.shared.web.CorrelationIdFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfiguration {

    @Bean
    SecurityErrorResponseWriter securityErrorResponseWriter(ObjectMapper objectMapper) {
        return new SecurityErrorResponseWriter(objectMapper);
    }

    @Bean
    GatewayAuthenticationEntryPoint gatewayAuthenticationEntryPoint(
            SecurityErrorResponseWriter errorResponseWriter
    ) {
        return new GatewayAuthenticationEntryPoint(errorResponseWriter);
    }

    @Bean
    GatewayAccessDeniedHandler gatewayAccessDeniedHandler(SecurityErrorResponseWriter errorResponseWriter) {
        return new GatewayAccessDeniedHandler(errorResponseWriter);
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            GatewayAuthenticationEntryPoint authenticationEntryPoint,
            GatewayAccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        CorrelationIdFilter correlationIdFilter = new CorrelationIdFilter();
        GatewayUserAuthenticationFilter gatewayUserAuthenticationFilter =
                new GatewayUserAuthenticationFilter(authenticationEntryPoint);
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasAuthority(UserRole.ROLE_ADMIN.name())
                        .requestMatchers("/api/v1/assessments", "/api/v1/assessments/**")
                        .hasAuthority(UserRole.ROLE_USER.name())
                        .requestMatchers("/api/v1/assessment-results", "/api/v1/assessment-results/**")
                        .hasAuthority(UserRole.ROLE_USER.name())
                        .requestMatchers("/api/v1/risk-alerts", "/api/v1/risk-alerts/**")
                        .hasAuthority(UserRole.ROLE_USER.name())
                        .requestMatchers("/api/v1/health-metrics", "/api/v1/health-metrics/**")
                        .hasAuthority(UserRole.ROLE_USER.name())
                        .requestMatchers("/api/v1/self-care-plan", "/api/v1/self-care-plan/**")
                        .hasAuthority(UserRole.ROLE_USER.name())
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(correlationIdFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(gatewayUserAuthenticationFilter, CorrelationIdFilter.class)
                .cors(Customizer.withDefaults())
                .build();
    }
}
