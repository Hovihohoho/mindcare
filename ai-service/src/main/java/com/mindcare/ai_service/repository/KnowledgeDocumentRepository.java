package com.mindcare.ai_service.repository;

import com.mindcare.ai_service.entity.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, UUID> {
}
