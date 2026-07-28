package com.mindcare.auth_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {
    private final Key signingKey;
    private final long expirationMs;

    public JwtUtil(@Value("${security.jwt.secret}") String secret,
                   @Value("${security.jwt.expiration-ms}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
    }

    public String generateToken(UUID userId, String email, String role) {
        long now = System.currentTimeMillis();
        return Jwts.builder().setSubject(email)
                .claim("userId", userId.toString())
                .claim("role", role)
                .setIssuedAt(new Date(now)).setExpiration(new Date(now + expirationMs))
                .signWith(signingKey).compact();
    }

    public String extractEmail(String token) { return claims(token).getSubject(); }
    public String extractRole(String token) { return claims(token).get("role", String.class); }
    public boolean isTokenValid(String token) { return claims(token).getExpiration().after(new Date()); }
    public long getExpirationSeconds() { return expirationMs / 1000; }

    private Claims claims(String token) {
        return Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token).getBody();
    }
}
