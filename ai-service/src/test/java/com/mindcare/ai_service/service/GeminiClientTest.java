package com.mindcare.ai_service.service;

import static org.assertj.core.api.Assertions.*;
import com.mindcare.ai_service.config.GeminiProperties;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

class GeminiClientTest {
    private HttpServer server;

    @AfterEach void stop() { if (server != null) server.stop(0); }

    private GeminiClient client(String fallback) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        return new GeminiClient(new GeminiProperties("test-key",
                "http://127.0.0.1:" + server.getAddress().getPort(), "embed", "primary", fallback, 768),
                new ObjectMapper());
    }

    @Test void providerFailureUsesConfiguredFallback() throws Exception {
        var client = client("fallback");
        var calls = new AtomicInteger();
        server.createContext("/models/primary:generateContent", exchange -> {
            calls.incrementAndGet();
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        });
        server.createContext("/models/fallback:generateContent", exchange -> {
            calls.incrementAndGet();
            byte[] body = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Hello\"}]}}]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        assertThat(client.generate("system", "prompt")).isEqualTo("Hello");
        assertThat(calls).hasValue(2);
    }

    @Test void readTimeoutBecomesControlledUnavailableError() throws Exception {
        var client = client("primary");
        ReflectionTestUtils.setField(client, "readTimeoutSeconds", 1);
        server.createContext("/models/primary:generateContent", exchange -> {
            try { Thread.sleep(1800); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            exchange.close();
        });
        assertThatThrownBy(() -> client.generate("system", "prompt"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("khả dụng");
    }

    @Test void emptyProviderBodyIsControlled() throws Exception {
        var client = client("primary");
        server.createContext("/models/primary:generateContent", exchange -> {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        assertThatThrownBy(() -> client.generate("system", "prompt"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("nội dung");
    }
}
