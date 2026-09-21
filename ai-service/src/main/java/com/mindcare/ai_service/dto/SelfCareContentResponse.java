package com.mindcare.ai_service.dto;

import com.mindcare.ai_service.entity.KnowledgeDocument;
import java.time.Instant;
import java.util.UUID;

public record SelfCareContentResponse(
        UUID id,
        String title,
        String content,
        String sourceUrl,
        String publisher,
        String evidenceScope,
        String limitation,
        Instant updatedAt
) {
    public static SelfCareContentResponse from(KnowledgeDocument document) {
        return new SelfCareContentResponse(
                document.getId(), document.getTitle(), document.getContent(), document.getSourceUrl(),
                document.getPublisher(), document.getEvidenceScope(), document.getLimitation(),
                document.getUpdatedAt());
    }
}
