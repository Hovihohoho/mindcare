package com.mindcare.bookingservice.integration.outbox.repository;

import com.mindcare.bookingservice.integration.outbox.entity.OutboxEvent;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.util.List;
import com.mindcare.bookingservice.integration.outbox.entity.OutboxStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT event FROM OutboxEvent event
            WHERE event.status IN :statuses
              AND event.eventType IN :eventTypes
              AND (event.nextAttemptAt IS NULL OR event.nextAttemptAt <= :now)
              AND event.deletedAt IS NULL
            ORDER BY event.createdAt ASC
            """)
    List<OutboxEvent> findNotificationEventsForUpdate(
            @Param("statuses") List<OutboxStatus> statuses,
            @Param("eventTypes") List<String> eventTypes,
            @Param("now") OffsetDateTime now,
            Pageable pageable);
}
