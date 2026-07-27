package com.mindcare.bookingservice.shared.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            VerifiedHeaderAuthenticationFilter verifiedHeaderAuthenticationFilter)
            throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/payment-webhooks/**").permitAll()
                        .requestMatchers("/api/v1/experts/*/schedules").permitAll()
                        .requestMatchers("/api/v1/experts/*/reviews").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(
                        verifiedHeaderAuthenticationFilter,
                        AnonymousAuthenticationFilter.class)
                .build();
    }
}
