package com.mindcare.bookingservice.integration.outbox.repository;

import com.mindcare.bookingservice.integration.outbox.entity.OutboxEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
}
