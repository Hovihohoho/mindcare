package com.mindcare.api_gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtIdentityFilter implements GlobalFilter, Ordered {

    static final String USER_ID_HEADER = "X-User-Id";
    static final String USER_ROLE_HEADER = "X-User-Role";

    private final Key signingKey;

    public JwtIdentityFilter(@Value("${security.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var headers = exchange.getRequest().mutate()
                .headers(values -> {
                    values.remove(USER_ID_HEADER);
                    values.remove(USER_ROLE_HEADER);
                });

        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(signingKey)
                        .build()
                        .parseClaimsJws(authorization.substring(7))
                        .getBody();
                String userId = claims.get("userId", String.class);
                String role = claims.get("role", String.class);
                UUID.fromString(userId);
                if (role == null || !role.matches("ROLE_(USER|EXPERT|ADMIN)")) {
                    throw new IllegalArgumentException("Unsupported role");
                }
                headers.header(USER_ID_HEADER, userId);
                headers.header(USER_ROLE_HEADER, role);
            } catch (JwtException | IllegalArgumentException ignored) {
                // Downstream services return 401 because no verified identity is forwarded.
            }
        }

        return chain.filter(exchange.mutate().request(headers.build()).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
