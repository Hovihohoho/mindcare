package com.mindcare.emotionservice.healthmetric.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(name = "health_source_consents", schema = "emotion_schema")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HealthSourceConsentEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "user_id", nullable = false, updatable = false) private UUID userId;
    @Column(name = "source_type", nullable = false, updatable = false, length = 50) private String sourceType;
    @Column(nullable = false) private boolean enabled;
    @Column(name = "revoked_at") private OffsetDateTime revokedAt;
    @CreatedDate @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @LastModifiedDate @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;

    public HealthSourceConsentEntity(UUID userId, String sourceType) {
        this.userId = userId; this.sourceType = sourceType; this.enabled = true;
    }
    public void enable() { enabled = true; revokedAt = null; }
    public void revoke(OffsetDateTime at) { enabled = false; revokedAt = at; }
}
