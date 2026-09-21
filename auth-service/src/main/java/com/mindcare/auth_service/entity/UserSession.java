package com.mindcare.auth_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "user_sessions", schema = "auth_schema")
public class UserSession {
    @Id
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;
    @Column(name = "user_agent")
    private String userAgent;
    @Column(name = "ip_address")
    private String ipAddress;
    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();
    @Column(name = "last_seen_at")
    private OffsetDateTime lastSeenAt = OffsetDateTime.now();
    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;
    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;
}
