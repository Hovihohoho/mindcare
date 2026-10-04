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
    @Test void joinsAnswerPartsAndExcludesThoughts() throws Exception {
        var client = client("primary");
        respond("{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":[{\"thought\":true,\"text\":\"private\"},{\"text\":\"Hello \"},{\"text\":\"world\"}]}}]}");
        assertThat(client.generate("system", "prompt")).isEqualTo("Hello world");
    }

    @Test void refusesTruncatedAnswers() throws Exception {
        var client = client("primary");
        respond("{\"candidates\":[{\"finishReason\":\"MAX_TOKENS\",\"content\":{\"parts\":[{\"text\":\"partial\"}]}}]}");
        assertThatThrownBy(() -> client.generate("system", "prompt")).hasMessageContaining("Incomplete");
    }

    @Test void safetyBlockDoesNotInvokeFallback() throws Exception {
        var client = client("fallback");
        var calls = new AtomicInteger();
        server.createContext("/models/fallback:generateContent", exchange -> {
            calls.incrementAndGet(); exchange.sendResponseHeaders(503, -1); exchange.close();
        });
        respond("{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}");
        assertThatThrownBy(() -> client.generate("system", "prompt")).hasMessageContaining("declined");
        assertThat(calls).hasValue(0);
    }

    private void respond(String json) {
        server.createContext("/models/primary:generateContent", exchange -> {
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
    }

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
        ReflectionTestUtils.setField(client, "maxAttempts", 1);
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

    @Test void transientProviderFailureRetriesSameModel() throws Exception {
        var client = client("primary");
        ReflectionTestUtils.setField(client, "retryDelayMs", 0L);
        var calls = new AtomicInteger();
        server.createContext("/models/primary:generateContent", exchange -> {
            if (calls.incrementAndGet() == 1) {
                exchange.sendResponseHeaders(503, -1);
                exchange.close();
                return;
            }
            byte[] body = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"Recovered\"}]}}]}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        assertThat(client.generate("system", "prompt")).isEqualTo("Recovered");
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
