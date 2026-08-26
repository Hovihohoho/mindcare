package com.mindcare.ai_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ai_conversation_messages", schema = "ai_schema")
public class AiConversationMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;
    @Column(nullable = false, length = 20)
    private String role;
    @Column(nullable = false, columnDefinition = "text")
    private String content;
    @Column(name = "sources_json", columnDefinition = "text")
    private String sourcesJson;
    @Column(name = "safety_level", nullable = false, length = 20)
    private String safetyLevel = "NONE";
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void createTimestamp() {
        createdAt = Instant.now();
    }
}
