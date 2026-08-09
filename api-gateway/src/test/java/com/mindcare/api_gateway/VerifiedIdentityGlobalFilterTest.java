package com.mindcare.api_gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

class VerifiedIdentityGlobalFilterTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void removesSpoofedIdentityHeadersFromAnonymousRequest() {
        var filter = new VerifiedIdentityGlobalFilter("http://127.0.0.1:1");
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/assessments")
                .header("X-User-Id", UUID.randomUUID().toString())
                .header("X-User-Role", "ROLE_ADMIN")
                .build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, current -> {
            forwarded.set(current);
            return current.getResponse().setComplete();
        }).block();

        assertThat(forwarded.get()).isNotNull();
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-User-Id")).isNull();
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-User-Role")).isNull();
    }

    @Test
    void validatesWebSocketTokenAndRemovesItFromForwardedUrl() throws Exception {
        UUID userId = UUID.randomUUID();
        AtomicReference<String> authorization = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/auth/me", request -> {
            authorization.set(request.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION));
            byte[] body = ("{\"success\":true,\"data\":{\"id\":\"" + userId
                    + "\",\"role\":\"ROLE_USER\"}}").getBytes(StandardCharsets.UTF_8);
            request.getResponseHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json");
            request.sendResponseHeaders(200, body.length);
            request.getResponseBody().write(body);
            request.close();
        });
        server.start();
        var filter = new VerifiedIdentityGlobalFilter(
                "http://127.0.0.1:" + server.getAddress().getPort());
        var exchange = MockServerWebExchange.from(MockServerHttpRequest
                .get("/ws/notifications?access_token=socket-token&client=web")
                .header("X-User-Role", "ROLE_ADMIN")
                .build());
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        filter.filter(exchange, current -> {
            forwarded.set(current);
            return current.getResponse().setComplete();
        }).block();

        assertThat(authorization.get()).isEqualTo("Bearer socket-token");
        assertThat(forwarded.get().getRequest().getURI().getQuery()).isEqualTo("client=web");
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-User-Id"))
                .isEqualTo(userId.toString());
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-User-Role"))
                .isEqualTo("ROLE_USER");
    }

    @Test
    void rejectsTokenWhenAuthServiceRejectsIt() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/auth/me", request -> {
            request.sendResponseHeaders(401, -1);
            request.close();
        });
        server.start();
        var filter = new VerifiedIdentityGlobalFilter(
                "http://127.0.0.1:" + server.getAddress().getPort());
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/bookings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer revoked-token")
                .build());
        AtomicBoolean forwarded = new AtomicBoolean();

        filter.filter(exchange, current -> {
            forwarded.set(true);
            return current.getResponse().setComplete();
        }).block();

        assertThat(forwarded).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
