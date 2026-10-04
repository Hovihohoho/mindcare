package com.mindcare.auth_service.notification;

import tools.jackson.databind.JsonNode;
import com.mindcare.auth_service.repository.ExpoPushReceiptRepository;
import com.mindcare.auth_service.repository.PushDeviceRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class ExpoPushReceiptScheduler {
    private static final Logger log = LoggerFactory.getLogger(ExpoPushReceiptScheduler.class);
    private final ExpoPushReceiptRepository receipts;
    private final PushDeviceRepository devices;
    private final RestClient.Builder restClientBuilder;

    @Value("${app.push.enabled:false}") private boolean enabled;
    @Value("${app.push.expo-receipts-url:https://exp.host/--/api/v2/push/getReceipts}") private String receiptsUrl;

    @Scheduled(fixedDelayString = "${app.push.receipt-poll-ms:300000}")
    @Transactional
    public void poll() {
        if (!enabled) return;
        receipts.deleteByStatusNotAndCreatedAtBefore("PENDING", OffsetDateTime.now().minusDays(30));
        var pending = receipts.findTop100ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
                "PENDING", OffsetDateTime.now().minusMinutes(15));
        if (pending.isEmpty()) return;
        try {
            JsonNode response = restClientBuilder.build().post()
                    .uri(receiptsUrl)
                    .body(Map.of("ids", pending.stream().map(item -> item.getTicketId()).toList()))
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode data = response == null ? null : response.path("data");
            if (data == null || !data.isObject()) return;
            for (var receipt : pending) {
                JsonNode providerReceipt = data.get(receipt.getTicketId());
                if (providerReceipt == null) continue;
                String status = providerReceipt.path("status").asText("error");
                String error = providerReceipt.path("details").path("error").asText(null);
                receipt.complete(status, error);
                if ("DeviceNotRegistered".equals(error)) {
                    devices.findById(receipt.getDeviceId()).ifPresent(device -> {
                        device.disable();
                        devices.save(device);
                    });
                }
            }
            receipts.saveAll(pending);
        } catch (RuntimeException exception) {
            log.warn("expo_receipt_poll_failed batchSize={} error={}",
                    pending.size(), exception.getClass().getSimpleName());
        }
    }
}
