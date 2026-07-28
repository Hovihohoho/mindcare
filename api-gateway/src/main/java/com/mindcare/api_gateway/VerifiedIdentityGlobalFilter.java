package com.mindcare.api_gateway;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class VerifiedIdentityGlobalFilter implements GlobalFilter, Ordered {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    private final WebClient authClient;

    public VerifiedIdentityGlobalFilter(
            @Value("${mindcare.auth-service-uri}") String authServiceUri
    ) {
        this.authClient = WebClient.builder().baseUrl(authServiceUri).build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerWebExchange sanitizedExchange = exchange.mutate()
                .request(request -> request.headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    headers.remove(USER_ROLE_HEADER);
                }))
                .build();

        String authorization = sanitizedExchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null
                || !authorization.startsWith("Bearer ")
                || sanitizedExchange.getRequest().getPath().value().startsWith("/api/auth/")) {
            return chain.filter(sanitizedExchange);
        }

        return authClient.get()
                .uri("/api/auth/me")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .bodyToMono(AuthEnvelope.class)
                .flatMap(envelope -> {
                    if (!envelope.success() || envelope.data() == null) {
                        return unauthorized(sanitizedExchange);
                    }
                    AuthUser user = envelope.data();
                    ServerWebExchange verifiedExchange = sanitizedExchange.mutate()
                            .request(request -> request.headers(headers -> {
                                headers.set(USER_ID_HEADER, user.id().toString());
                                headers.set(USER_ROLE_HEADER, user.role());
                            }))
                            .build();
                    return chain.filter(verifiedExchange);
                })
                .onErrorResume(WebClientResponseException.Unauthorized.class,
                        ignored -> unauthorized(sanitizedExchange))
                .onErrorResume(WebClientResponseException.Forbidden.class,
                        ignored -> unauthorized(sanitizedExchange))
                .onErrorResume(WebClientResponseException.class,
                        ignored -> serviceUnavailable(sanitizedExchange));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private Mono<Void> serviceUnavailable(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    private record AuthEnvelope(boolean success, AuthUser data) {
    }

    private record AuthUser(UUID id, String role) {
    }
}
