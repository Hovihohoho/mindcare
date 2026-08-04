package com.mindcare.bookingservice.integration.outbox.entity;

import com.mindcare.bookingservice.shared.entity.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "outbox_events", schema = "booking_schema")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent extends AuditableEntity {

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 150)
    private String eventType;

    @Column(name = "event_version", nullable = false)
    private int eventVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at")
    private OffsetDateTime nextAttemptAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    public static OutboxEvent pending(
            String aggregateType,
            UUID aggregateId,
            String eventType,
            String payload) {
        OutboxEvent event = new OutboxEvent();
        event.aggregateType = aggregateType;
        event.aggregateId = aggregateId;
        event.eventType = eventType;
        event.eventVersion = 1;
        event.payload = payload;
        event.status = OutboxStatus.PENDING;
        event.attemptCount = 0;
        return event;
    }

    public void markPublished(OffsetDateTime now) {
        status = OutboxStatus.PUBLISHED;
        publishedAt = now;
        nextAttemptAt = null;
    }

    public void markFailed(OffsetDateTime nextAttemptAt) {
        status = OutboxStatus.FAILED;
        attemptCount++;
        this.nextAttemptAt = nextAttemptAt;
    }
}
