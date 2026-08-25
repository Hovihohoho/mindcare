package com.mindcare.ai_service.service;

import com.mindcare.ai_service.dto.SelfCareContentResponse;
import com.mindcare.ai_service.entity.KnowledgeDocument;
import com.mindcare.ai_service.repository.KnowledgeDocumentRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SelfCareContentService {
    public static final String DOCUMENT_TYPE = "SELF_CARE_ARTICLE";
    private final KnowledgeDocumentRepository repository;

    @Transactional(readOnly = true)
    public List<SelfCareContentResponse> list() {
        List<KnowledgeDocument> withoutExpiry = repository
                .findByDocumentTypeAndActiveTrueAndReviewStatusAndProcessingStatusAndExpiresAtIsNullOrderByUpdatedAtDesc(
                        DOCUMENT_TYPE, "APPROVED", "READY");
        List<KnowledgeDocument> withFutureExpiry = repository
                .findByDocumentTypeAndActiveTrueAndReviewStatusAndProcessingStatusAndExpiresAtAfterOrderByUpdatedAtDesc(
                        DOCUMENT_TYPE, "APPROVED", "READY", Instant.now());
        return java.util.stream.Stream.concat(withoutExpiry.stream(), withFutureExpiry.stream())
                .sorted(java.util.Comparator.comparing(KnowledgeDocument::getUpdatedAt).reversed())
                .map(SelfCareContentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SelfCareContentResponse find(UUID id) {
        KnowledgeDocument document = repository.findById(id)
                .filter(this::isPublishedSelfCareContent)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nội dung tự chăm sóc"));
        return SelfCareContentResponse.from(document);
    }

    private boolean isPublishedSelfCareContent(KnowledgeDocument document) {
        return DOCUMENT_TYPE.equals(document.getDocumentType())
                && Boolean.TRUE.equals(document.getActive())
                && "APPROVED".equals(document.getReviewStatus())
                && "READY".equals(document.getProcessingStatus())
                && (document.getExpiresAt() == null || document.getExpiresAt().isAfter(Instant.now()));
    }
}
