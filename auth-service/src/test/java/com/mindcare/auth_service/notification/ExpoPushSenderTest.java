package com.mindcare.auth_service.notification;

import com.mindcare.auth_service.entity.PushDevice;
import com.mindcare.auth_service.repository.ExpoPushReceiptRepository;
import com.mindcare.auth_service.repository.PushDeviceRepository;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExpoPushSenderTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void sendsPrivacySafeBodyAndKeepsAcceptedDeviceEnabled() throws Exception {
        AtomicReference<String> requestBody = startServer("""
                {"data":{"status":"ok","id":"ticket-1"}}
                """);
        PushDeviceRepository repository = Mockito.mock(PushDeviceRepository.class);
        ExpoPushReceiptRepository receipts = Mockito.mock(ExpoPushReceiptRepository.class);
        PushDevice device = PushDevice.create(UUID.randomUUID(), "installation", "ExponentPushToken[test]", "ANDROID");
        when(repository.findByUserIdAndEnabledTrue(device.getUserId())).thenReturn(List.of(device));
        var sender = new ExpoPushSender(repository, receipts, RestClient.builder(), true, endpoint());

        sender.send(device.getUserId(), "A check-in", "Sensitive prediction detail", "/emotion", "DAILY_CHECK_IN");

        assertThat(requestBody.get()).contains("MindCare c\u00f3 m\u1ed9t l\u1eddi nh\u1eafc d\u00e0nh cho b\u1ea1n.")
                .doesNotContain("Sensitive prediction detail")
                .contains("\"url\":\"/emotion\"")
                .contains("\"type\":\"DAILY_CHECK_IN\"");
        assertThat(device.isEnabled()).isTrue();
        verify(repository, never()).save(device);
        verify(receipts).save(org.mockito.ArgumentMatchers.argThat(
                receipt -> receipt.getDeviceId().equals(device.getId()) && receipt.getTicketId().equals("ticket-1")));
    }

    @Test
    void disablesDeviceWhenExpoRejectsAnExpiredToken() throws Exception {
        startServer("""
                {"data":{"status":"error","details":{"error":"DeviceNotRegistered"}}}
                """);
        PushDeviceRepository repository = Mockito.mock(PushDeviceRepository.class);
        ExpoPushReceiptRepository receipts = Mockito.mock(ExpoPushReceiptRepository.class);
        PushDevice device = PushDevice.create(UUID.randomUUID(), "installation", "ExponentPushToken[expired]", "ANDROID");
        when(repository.findByUserIdAndEnabledTrue(device.getUserId())).thenReturn(List.of(device));
        var sender = new ExpoPushSender(repository, receipts, RestClient.builder(), true, endpoint());

        sender.send(device.getUserId(), "Title", "Body", "/emotion", "DAILY_CHECK_IN");

        assertThat(device.isEnabled()).isFalse();
        verify(repository).save(device);
    }

    @Test
    void rejectedTicketPropagatesFailureForDurableRetry() throws Exception {
        startServer("""
                {"data":{"status":"error","details":{"error":"MessageRateExceeded"}}}
                """);
        PushDeviceRepository repository = Mockito.mock(PushDeviceRepository.class);
        ExpoPushReceiptRepository receipts = Mockito.mock(ExpoPushReceiptRepository.class);
        PushDevice device = PushDevice.create(UUID.randomUUID(), "installation", "ExponentPushToken[test]", "ANDROID");
        when(repository.findByUserIdAndEnabledTrue(device.getUserId())).thenReturn(List.of(device));
        var sender = new ExpoPushSender(repository, receipts, RestClient.builder(), true, endpoint());

        assertThatThrownBy(() -> sender.send(device.getUserId(), "Title", "Body", "/emotion", "DAILY_CHECK_IN"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(device.isEnabled()).isTrue();
        verify(receipts, never()).save(Mockito.any());
    }

    private AtomicReference<String> startServer(String response) throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/push", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        return requestBody;
    }

    private String endpoint() {
        return "http://localhost:" + server.getAddress().getPort() + "/push";
    }
}
