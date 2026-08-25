package com.mindcare.ai_service.repository;

import com.mindcare.ai_service.entity.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, UUID> {
    List<KnowledgeDocument> findByDocumentTypeAndActiveTrueAndReviewStatusAndProcessingStatusAndExpiresAtIsNullOrderByUpdatedAtDesc(
            String documentType, String reviewStatus, String processingStatus);

    List<KnowledgeDocument> findByDocumentTypeAndActiveTrueAndReviewStatusAndProcessingStatusAndExpiresAtAfterOrderByUpdatedAtDesc(
            String documentType, String reviewStatus, String processingStatus, Instant now);
}
