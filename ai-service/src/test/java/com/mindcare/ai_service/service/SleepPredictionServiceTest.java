package com.mindcare.ai_service.service;

import com.mindcare.ai_service.dto.SleepPredictionRequest;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SleepPredictionServiceTest {
    private HttpServer server;
    private final AtomicReference<String> body = new AtomicReference<>();

    private SleepPredictionService service(int status, String response) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/predict", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        return new SleepPredictionService("http://127.0.0.1:" + server.getAddress().getPort());
    }

    private SleepPredictionRequest request() {
        return new SleepPredictionRequest(LocalDate.of(2024, 2, 29), 10000, 20, 15, 200, 700, 420);
    }

    @AfterEach
    void stop() {
        if (server != null) server.stop(0);
    }

    @Test
    void forwardsMeasurementsAndReturnsNextDay() throws Exception {
        var client = service(200, """
                {"date":"2024-02-29","targetDate":"2024-03-01",
                 "predictedSleepMinutes":430.25,"modelType":"RandomForestRegressor"}
                """);
        var result = client.predict(request());
        assertEquals(430.25, result.predictedSleepMinutes());
        assertEquals(LocalDate.of(2024, 3, 1), result.targetDate());
        assertTrue(body.get().contains("\"totalSteps\":10000"));
        assertTrue(body.get().contains("\"date\":\"2024-02-29\""));
    }

    @Test
    void unavailableInferenceDoesNotFabricatePrediction() throws Exception {
        var client = service(503, "{\"detail\":\"model missing\"}");
        assertThrows(IllegalStateException.class, () -> client.predict(request()));
    }

    @Test
    void rejectsWrongTargetDate() throws Exception {
        var client = service(200, """
                {"date":"2024-02-29","targetDate":"2024-03-02",
                 "predictedSleepMinutes":430,"modelType":"RandomForestRegressor"}
                """);
        assertThrows(IllegalStateException.class, () -> client.predict(request()));
    }
}
