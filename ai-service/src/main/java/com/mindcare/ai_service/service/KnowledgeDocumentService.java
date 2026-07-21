package com.mindcare.ai_service.service;

import com.mindcare.ai_service.dto.KnowledgeDocumentRequest;
import com.mindcare.ai_service.entity.KnowledgeDocument;
import com.mindcare.ai_service.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KnowledgeDocumentService {
    private final KnowledgeDocumentRepository repository;

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
        return document;
    }
}
