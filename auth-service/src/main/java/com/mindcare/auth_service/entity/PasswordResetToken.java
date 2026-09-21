package com.mindcare.auth_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "password_reset_tokens", schema = "auth_schema")
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;
    @Column(name = "token_hash", unique = true, nullable = false)
    private String tokenHash;
    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;
    @Column(name = "used_at")
    private OffsetDateTime usedAt;
    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
