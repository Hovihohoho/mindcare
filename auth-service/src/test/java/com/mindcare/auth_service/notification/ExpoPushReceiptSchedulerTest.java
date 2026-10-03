package com.mindcare.auth_service.notification;

import com.mindcare.auth_service.entity.ExpoPushReceipt;
import com.mindcare.auth_service.entity.PushDevice;
import com.mindcare.auth_service.repository.ExpoPushReceiptRepository;
import com.mindcare.auth_service.repository.PushDeviceRepository;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExpoPushReceiptSchedulerTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void recordsReceiptFailureAndDisablesExpiredDevice() throws Exception {
        ExpoPushReceiptRepository receipts = Mockito.mock(ExpoPushReceiptRepository.class);
        PushDeviceRepository devices = Mockito.mock(PushDeviceRepository.class);
        PushDevice device = PushDevice.create(UUID.randomUUID(), "installation", "ExponentPushToken[old]", "ANDROID");
        ExpoPushReceipt receipt = ExpoPushReceipt.pending(device.getId(), "ticket-expired");
        when(receipts.findTop100ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
                ArgumentMatchers.eq("PENDING"), ArgumentMatchers.any(OffsetDateTime.class)))
                .thenReturn(List.of(receipt));
        when(devices.findById(device.getId())).thenReturn(Optional.of(device));
        String endpoint = startServer("""
                {"data":{"ticket-expired":{"status":"error","details":{"error":"DeviceNotRegistered"}}}}
                """);
        var scheduler = new ExpoPushReceiptScheduler(receipts, devices, RestClient.builder());
        ReflectionTestUtils.setField(scheduler, "enabled", true);
        ReflectionTestUtils.setField(scheduler, "receiptsUrl", endpoint);

        scheduler.poll();

        assertThat(receipt.getStatus()).isEqualTo("FAILED");
        assertThat(receipt.getErrorCode()).isEqualTo("DeviceNotRegistered");
        assertThat(device.isEnabled()).isFalse();
        verify(devices).save(device);
        verify(receipts).saveAll(List.of(receipt));
    }

    private String startServer(String response) throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/receipts", exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        return "http://localhost:" + server.getAddress().getPort() + "/receipts";
    }
}
