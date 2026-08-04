package com.mindcare.ai_service.service;

import com.mindcare.ai_service.dto.KnowledgeDocumentRequest;
import com.mindcare.ai_service.entity.KnowledgeDocument;
import com.mindcare.ai_service.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KnowledgeDocumentService {
    private final KnowledgeDocumentRepository repository;
    private final EmbeddingService embeddingService;
    private final DocumentTextExtractor textExtractor;

    @Transactional(readOnly = true)
    public List<KnowledgeDocument> findAll() { return repository.findAll(); }

    @Transactional(readOnly = true)
    public KnowledgeDocument findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu AI"));
    }

    @Transactional
    public KnowledgeDocument create(KnowledgeDocumentRequest request) {
        KnowledgeDocument saved = repository.saveAndFlush(apply(new KnowledgeDocument(), request));
        return index(saved);
    }

    @Transactional
    public KnowledgeDocument update(UUID id, KnowledgeDocumentRequest request) {
        KnowledgeDocument saved = repository.saveAndFlush(apply(findById(id), request));
        return index(saved);
    }

    @Transactional
    public KnowledgeDocument upload(MultipartFile file, String title, String sourceUrl,
                                    String documentType) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setTitle(title == null || title.isBlank()
                ? safeFilename(file.getOriginalFilename()) : title.trim());
        try {
            document.setContent(textExtractor.extract(file));
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new RuntimeException("Không thể đọc nội dung tài liệu", exception);
        }
        document.setSourceUrl(blankToNull(sourceUrl));
        document.setDocumentType(documentType == null || documentType.isBlank()
                ? "KNOWLEDGE" : documentType.trim().toUpperCase());
        document.setOriginalFilename(safeFilename(file.getOriginalFilename()));
        document.setMimeType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setActive(true);
        return index(repository.saveAndFlush(document));
    }

    @Transactional
    public KnowledgeDocument reindex(UUID id) {
        return index(findById(id));
    }

    @Transactional
    public void delete(UUID id) {
        KnowledgeDocument document = findById(id);
        repository.delete(document);
    }

    private KnowledgeDocument apply(KnowledgeDocument document, KnowledgeDocumentRequest request) {
        document.setTitle(request.title().trim());
        document.setContent(request.content());
        document.setSourceUrl(request.sourceUrl());
        document.setDocumentType(request.documentType());
        if (request.active() != null) document.setActive(request.active());
        document.setProcessingStatus("PROCESSING");
        document.setProcessingError(null);
        return document;
    }

    private KnowledgeDocument index(KnowledgeDocument document) {
        document.setProcessingStatus("PROCESSING");
        document.setProcessingError(null);
        repository.saveAndFlush(document);
        try {
            embeddingService.embed(document);
            document.setProcessingStatus("READY");
            document.setIndexedAt(Instant.now());
        } catch (RuntimeException exception) {
            document.setProcessingStatus("FAILED");
            document.setProcessingError(trim(exception.getMessage(), 2000));
        }
        return repository.save(document);
    }

    private String safeFilename(String value) {
        if (value == null || value.isBlank()) return "Tài liệu không tên";
        String normalized = value.replace('\\', '/');
        normalized = normalized.substring(normalized.lastIndexOf('/') + 1);
        return trim(normalized, 255);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String trim(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
