package com.mindcare.ai_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "knowledge_documents", schema = "ai_schema")
public class KnowledgeDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false, columnDefinition = "text")
    private String content;
    @Column(name = "source_url", length = 1000)
    private String sourceUrl;
    @Column(name = "document_type", length = 50)
    private String documentType;
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;
    @Column(name = "original_filename")
    private String originalFilename;
    @Column(name = "mime_type")
    private String mimeType;
    @Column(name = "file_size")
    private Long fileSize;
    @Column(name = "processing_status", nullable = false)
    private String processingStatus = "PROCESSING";
    @Column(name = "processing_error", length = 2000)
    private String processingError;
    @Column(name = "indexed_at")
    private Instant indexedAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void createTimestamps() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }
    @PreUpdate
    void updateTimestamp() { updatedAt = Instant.now(); }
}
