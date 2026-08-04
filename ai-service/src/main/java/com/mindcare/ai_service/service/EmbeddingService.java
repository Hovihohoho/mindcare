package com.mindcare.ai_service.service;

import com.mindcare.ai_service.entity.KnowledgeDocument;
import com.mindcare.ai_service.repository.KnowledgeDocumentRepository;
import com.mindcare.ai_service.repository.KnowledgeVectorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmbeddingService {
    private final GeminiClient geminiClient;
    private final KnowledgeVectorRepository vectorRepository;
    private final KnowledgeDocumentRepository documentRepository;

    public void embed(KnowledgeDocument document) {
        String input = document.getTitle() + "\n\n" + document.getContent();
        vectorRepository.updateEmbedding(document.getId(), geminiClient.embed(input, "RETRIEVAL_DOCUMENT"));
    }

    public List<KnowledgeVectorRepository.SimilarityResult> search(String query, int limit, double threshold) {
        return vectorRepository.search(geminiClient.embed(query, "RETRIEVAL_QUERY"), limit, threshold);
    }

    @Transactional
    public int reindexAll() {
        List<KnowledgeDocument> documents = documentRepository.findAll().stream()
                .filter(document -> Boolean.TRUE.equals(document.getActive())).toList();
        documents.forEach(this::embed);
        return documents.size();
    }

    @Transactional
    public void reindex(UUID documentId) {
        embed(documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu AI")));
    }
}
