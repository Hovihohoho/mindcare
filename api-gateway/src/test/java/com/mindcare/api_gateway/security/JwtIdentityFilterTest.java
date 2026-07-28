package com.mindcare.api_gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

class JwtIdentityFilterTest {

    private static final String SECRET =
            "TWluZENhcmUtRGV2ZWxvcG1lbnQtSldULVNlY3JldC0yMDI2IQ==";

    private final JwtIdentityFilter filter = new JwtIdentityFilter(SECRET);

    @Test
    void signedLegacyTokenWithoutUserIdDoesNotFailTheRequest() {
        String token = Jwts.builder()
                .setSubject("user@example.com")
                .claim("role", "ROLE_USER")
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .compact();
        MockServerWebExchange exchange = exchangeWithToken(token);
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, current -> {
            forwarded.set(current);
            return current.getResponse().setComplete();
        }).block();

        assertThat(forwarded.get()).isNotNull();
        assertThat(forwarded.get().getRequest().getHeaders()
                .getFirst(JwtIdentityFilter.USER_ID_HEADER)).isNull();
        assertThat(forwarded.get().getRequest().getHeaders()
                .getFirst(JwtIdentityFilter.USER_ROLE_HEADER)).isNull();
    }

    private MockServerWebExchange exchangeWithToken(String token) {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/assessments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build());
    }
}
