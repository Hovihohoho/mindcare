package com.mindcare.auth_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "expert_documents", schema = "auth_schema")
public class ExpertDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;
    @Column(name = "document_type")
    private String documentType;
    private String title;
    @Column(name = "file_url")
    private String fileUrl;
    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
