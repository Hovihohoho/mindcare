package com.mindcare.bookingservice.integration.outbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindcare.bookingservice.integration.outbox.entity.OutboxEvent;
import com.mindcare.bookingservice.integration.outbox.repository.OutboxEventRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxServiceImpl implements OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void append(
            String aggregateType,
            UUID aggregateId,
            String eventType,
            Map<String, Object> payload) {
        try {
            outboxEventRepository.save(
                    OutboxEvent.pending(
                            aggregateType,
                            aggregateId,
                            eventType,
                            objectMapper.writeValueAsString(payload)));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize outbox payload", exception);
        }
    }
}
