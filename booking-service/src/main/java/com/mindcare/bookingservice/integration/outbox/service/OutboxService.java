package com.mindcare.bookingservice.integration.outbox.service;

import java.util.Map;
import java.util.UUID;

public interface OutboxService {

    void append(
            String aggregateType,
            UUID aggregateId,
            String eventType,
            Map<String, Object> payload);
}
