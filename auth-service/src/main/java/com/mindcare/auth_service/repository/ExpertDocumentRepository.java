package com.mindcare.auth_service.repository;

import com.mindcare.auth_service.entity.ExpertDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ExpertDocumentRepository extends JpaRepository<ExpertDocument, UUID> {
    List<ExpertDocument> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
