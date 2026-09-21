package com.mindcare.auth_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "admin_audit_logs", schema = "auth_schema")
public class AdminAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_user_id")
    private User admin;
    private String action;
    @Column(name = "target_type")
    private String targetType;
    @Column(name = "target_id")
    private String targetId;
    @Column(columnDefinition = "TEXT")
    private String detail;
    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
