package com.mindcare.auth_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "expo_push_receipts", schema = "auth_schema")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpoPushReceipt {
    @Id private UUID id;
    @Column(name = "device_id", nullable = false, updatable = false) private UUID deviceId;
    @Column(name = "ticket_id", nullable = false, unique = true, updatable = false, length = 120) private String ticketId;
    @Column(nullable = false, length = 20) private String status;
    @Column(name = "error_code", length = 80) private String errorCode;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "checked_at") private OffsetDateTime checkedAt;

    public static ExpoPushReceipt pending(UUID deviceId, String ticketId) {
        ExpoPushReceipt value = new ExpoPushReceipt();
        value.id = UUID.randomUUID();
        value.deviceId = deviceId;
        value.ticketId = ticketId;
        value.status = "PENDING";
        value.createdAt = OffsetDateTime.now();
        return value;
    }

    public void complete(String providerStatus, String errorCode) {
        status = "ok".equals(providerStatus) ? "DELIVERED" : "FAILED";
        this.errorCode = errorCode;
        checkedAt = OffsetDateTime.now();
    }
}
