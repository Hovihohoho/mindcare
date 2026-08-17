package com.mindcare.ai_service.service;

import com.mindcare.ai_service.dto.KnowledgeDocumentRequest;
import com.mindcare.ai_service.entity.KnowledgeDocument;
import com.mindcare.ai_service.repository.KnowledgeDocumentRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class KnowledgeDocumentService {
    private final KnowledgeDocumentRepository repository;
    private final EmbeddingService embeddingService;
    private final DocumentTextExtractor textExtractor;
    private final TrustedSourcePolicy trustedSourcePolicy;

    @Transactional(readOnly = true)
    public List<KnowledgeDocument> findAll() { return repository.findAll(); }

    @Transactional(readOnly = true)
    public KnowledgeDocument findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu AI"));
    }

    @Transactional
    public KnowledgeDocument create(KnowledgeDocumentRequest request) {
        return repository.save(apply(new KnowledgeDocument(), request));
    }

    @Transactional
    public KnowledgeDocument update(UUID id, KnowledgeDocumentRequest request) {
        return repository.save(apply(findById(id), request));
    }

    @Transactional
    public KnowledgeDocument upload(MultipartFile file, String title, String sourceUrl, String documentType) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setTitle(title == null || title.isBlank() ? safeFilename(file.getOriginalFilename()) : title.trim());
        try {
            document.setContent(textExtractor.extract(file));
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new RuntimeException("Không thể đọc nội dung tài liệu", exception);
        }
        document.setSourceUrl(requireSourceUrl(sourceUrl));
        document.setDocumentType(documentType == null || documentType.isBlank()
                ? "KNOWLEDGE" : documentType.trim().toUpperCase());
        document.setOriginalFilename(safeFilename(file.getOriginalFilename()));
        document.setMimeType(file.getContentType());
        document.setFileSize(file.getSize());
        markNeedsReview(document);
        return repository.save(document);
    }

    @Transactional
    public KnowledgeDocument enableForAi(UUID id, String reviewedBy) {
        KnowledgeDocument document = findById(id);
        TrustedSourcePolicy.SourceTrust trust = trustedSourcePolicy.verify(document.getSourceUrl());
        document.setPublisher(trust.publisher());
        document.setSourceTier(trust.tier());
        document.setEvidenceScope("Nội dung trong tài liệu phải được đối chiếu trực tiếp với đường dẫn nguồn đã gắn.");
        document.setLimitation("Chỉ dùng để cung cấp thông tin tham khảo; không thay thế chẩn đoán hoặc điều trị y khoa.");
        document.setReviewedBy(requireText(reviewedBy, "Người xác nhận"));
        document.setReviewedAt(Instant.now());
        document.setReviewStatus("APPROVED");
        document.setActive(true);
        return index(document);
    }

    @Transactional
    public KnowledgeDocument reject(UUID id, String reviewedBy) {
        KnowledgeDocument document = findById(id);
        document.setReviewStatus("REJECTED");
        document.setReviewedBy(requireText(reviewedBy, "Người kiểm duyệt"));
        document.setReviewedAt(Instant.now());
        document.setActive(false);
        document.setProcessingStatus("REJECTED");
        return repository.save(document);
    }

    @Transactional
    public KnowledgeDocument reindex(UUID id) {
        KnowledgeDocument document = findById(id);
        if (!isRetrievable(document)) {
            throw new IllegalStateException("Chỉ tài liệu nguồn cấp A/B đã được duyệt mới được lập chỉ mục");
        }
        return index(document);
    }

    @Transactional
    public void delete(UUID id) { repository.delete(findById(id)); }

    private KnowledgeDocument apply(KnowledgeDocument document, KnowledgeDocumentRequest request) {
        document.setTitle(request.title().trim());
        document.setContent(request.content());
        document.setSourceUrl(requireSourceUrl(request.sourceUrl()));
        document.setDocumentType(request.documentType());
        markNeedsReview(document);
        return document;
    }

    private void markNeedsReview(KnowledgeDocument document) {
        document.setActive(false);
        document.setReviewStatus("NEEDS_REVIEW");
        document.setSourceTier("UNRATED");
        document.setReviewedBy(null);
        document.setReviewedAt(null);
        document.setProcessingStatus("PENDING_REVIEW");
        document.setProcessingError(null);
        document.setIndexedAt(null);
    }

    private boolean isRetrievable(KnowledgeDocument document) {
        return "APPROVED".equals(document.getReviewStatus())
                && List.of("A", "B").contains(document.getSourceTier())
                && (document.getExpiresAt() == null || document.getExpiresAt().isAfter(Instant.now()));
    }

    private KnowledgeDocument index(KnowledgeDocument document) {
        document.setActive(true);
        document.setProcessingStatus("PROCESSING");
        document.setProcessingError(null);
        repository.saveAndFlush(document);
        try {
            embeddingService.embed(document);
            document.setProcessingStatus("READY");
            document.setIndexedAt(Instant.now());
        } catch (RuntimeException exception) {
            document.setActive(false);
            document.setProcessingStatus("FAILED");
            document.setProcessingError(trim(exception.getMessage(), 2000));
        }
        return repository.save(document);
    }

    private String safeFilename(String value) {
        if (value == null || value.isBlank()) return "Tài liệu không tên";
        String normalized = value.replace('\\', '/');
        return trim(normalized.substring(normalized.lastIndexOf('/') + 1), 255);
    }

    private String requireSourceUrl(String value) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || !normalized.startsWith("https://")) {
            throw new IllegalArgumentException("Tài liệu AI phải có đường dẫn nguồn HTTPS để kiểm chứng");
        }
        return normalized;
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " không được để trống");
        return value.trim();
    }

    private String trim(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
