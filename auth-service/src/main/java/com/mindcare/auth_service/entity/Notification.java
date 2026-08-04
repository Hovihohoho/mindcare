package com.mindcare.auth_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "notifications", schema = "auth_schema")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;
    private String title;
    @Column(columnDefinition = "TEXT")
    private String content;
    @Column(name = "notification_type")
    private String notificationType = "SYSTEM";
    @Column(name = "action_url")
    private String actionUrl;
    @Column(name = "read_at")
    private OffsetDateTime readAt;
    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
