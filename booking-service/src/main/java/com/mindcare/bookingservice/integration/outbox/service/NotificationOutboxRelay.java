package com.mindcare.bookingservice.integration.outbox.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindcare.bookingservice.integration.outbox.entity.OutboxEvent;
import com.mindcare.bookingservice.integration.outbox.entity.OutboxStatus;
import com.mindcare.bookingservice.integration.outbox.repository.OutboxEventRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@ConditionalOnProperty(name = "booking.notification.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationOutboxRelay {
    private static final List<String> TYPES = List.of(
            "booking.confirmed", "booking.canceled", "booking.cancellation-requested",
            "booking.cancellation-rejected", "booking.reminder-due");

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final RestClient client;
    private final String internalSecret;
    private final Clock clock;

    public NotificationOutboxRelay(
            OutboxEventRepository repository,
            ObjectMapper objectMapper,
            @Value("${booking.notification.base-url}") String baseUrl,
            @Value("${booking.notification.internal-secret}") String internalSecret,
            Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        this.internalSecret = internalSecret;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${booking.notification.fixed-delay:5000}")
    @Transactional
    public void publish() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        var events = repository.findNotificationEventsForUpdate(
                List.of(OutboxStatus.PENDING, OutboxStatus.FAILED), TYPES, now, PageRequest.of(0, 50));
        for (OutboxEvent event : events) {
            try {
                deliver(event);
                event.markPublished(now);
            } catch (RuntimeException exception) {
                long delaySeconds = Math.min(300, 1L << Math.min(event.getAttemptCount(), 8));
                event.markFailed(now.plusSeconds(delaySeconds));
                log.warn("Notification delivery failed for event {} (attempt {})",
                        event.getId(), event.getAttemptCount());
            }
        }
    }

    private void deliver(OutboxEvent event) {
        JsonNode payload;
        try {
            payload = objectMapper.readTree(event.getPayload());
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid outbox payload", exception);
        }
        switch (event.getEventType()) {
            case "booking.confirmed" -> {
                send(event, payload, "userId", "BOOKING_CONFIRMED", "Đặt lịch thành công",
                        "Lịch tư vấn của bạn đã được xác nhận.", "/");
                send(event, payload, "expertUserId", "BOOKING_CONFIRMED", "Có lịch tư vấn mới",
                        "Một khách hàng vừa đặt lịch tư vấn.", "/expert");
            }
            case "booking.canceled" -> {
                send(event, payload, "userId", "BOOKING_CANCELED", "Lịch tư vấn đã hủy",
                        "Lịch tư vấn đã được hủy. Vui lòng kiểm tra lại lịch của bạn.", "/");
                send(event, payload, "expertUserId", "BOOKING_CANCELED", "Lịch tư vấn đã hủy",
                        "Một lịch tư vấn đã được hủy.", "/expert");
            }
            case "booking.cancellation-requested" ->
                    send(event, payload, "expertUserId", "BOOKING_CHANGED", "Yêu cầu hủy lịch",
                            "Khách hàng đang chờ bạn xử lý yêu cầu hủy lịch.", "/expert");
            case "booking.cancellation-rejected" ->
                    send(event, payload, "userId", "BOOKING_CHANGED", "Lịch tư vấn không thay đổi",
                            "Yêu cầu hủy chưa được chấp thuận; lịch tư vấn vẫn giữ nguyên.", "/");
            case "booking.reminder-due" -> {
                String startAt = payload.path("startAt").asText();
                String bookingId = payload.path("bookingId").asText();
                send(event, payload, "userId", "BOOKING_REMINDER", "Sắp đến giờ tư vấn",
                        "Buổi tư vấn của bạn bắt đầu lúc " + startAt + ".", "/chat/" + bookingId);
                send(event, payload, "expertUserId", "BOOKING_REMINDER", "Sắp đến giờ tư vấn",
                        "Buổi tư vấn của bạn bắt đầu lúc " + startAt + ".", "/expert/chat/" + bookingId);
            }
            default -> { }
        }
    }

    private void send(OutboxEvent event, JsonNode payload, String recipientField,
                      String type, String title, String message, String actionUrl) {
        JsonNode recipient = payload.get(recipientField);
        if (recipient == null || recipient.isNull()) return;
        client.post()
                .uri("/api/auth/internal/notifications")
                .header("X-Internal-Secret", internalSecret)
                .body(Map.of("eventId", event.getId(), "userId", UUID.fromString(recipient.asText()),
                        "type", type, "title", title, "message", message, "actionUrl", actionUrl))
                .retrieve()
                .toBodilessEntity();
    }
}
