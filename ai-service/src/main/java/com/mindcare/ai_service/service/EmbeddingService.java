package com.mindcare.ai_service.service;

import com.mindcare.ai_service.entity.KnowledgeDocument;
import com.mindcare.ai_service.repository.KnowledgeDocumentRepository;
import com.mindcare.ai_service.repository.KnowledgeVectorRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmbeddingService {
    private final GeminiClient geminiClient;
    private final KnowledgeVectorRepository vectorRepository;
    private final KnowledgeDocumentRepository documentRepository;

    public void embed(KnowledgeDocument document) {
        String input = document.getTitle() + "\n\n" + document.getContent();
        vectorRepository.updateEmbedding(
                document.getId(), geminiClient.embed(input, "RETRIEVAL_DOCUMENT"));
    }

    public List<KnowledgeVectorRepository.SimilarityResult> search(
            String query, int limit, double threshold) {
        return vectorRepository.search(
                geminiClient.embed(query, "RETRIEVAL_QUERY"), limit, threshold);
    }

    @Transactional
    public int reindexAll() {
        List<KnowledgeDocument> documents = documentRepository.findAll().stream()
                .filter(document -> Boolean.TRUE.equals(document.getActive()))
                .toList();
        int indexed = 0;
        for (KnowledgeDocument document : documents) {
            if (reindexDocument(document)) indexed++;
        }
        return indexed;
    }

    @Transactional
    public void reindex(UUID documentId) {
        KnowledgeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài liệu AI"));
        if (!reindexDocument(document)) {
            throw new RuntimeException(document.getProcessingError());
        }
    }

    private boolean reindexDocument(KnowledgeDocument document) {
        document.setProcessingStatus("PROCESSING");
        document.setProcessingError(null);
        documentRepository.saveAndFlush(document);
        try {
            embed(document);
            document.setProcessingStatus("READY");
            document.setIndexedAt(Instant.now());
            documentRepository.save(document);
            return true;
        } catch (RuntimeException exception) {
            document.setProcessingStatus("FAILED");
            String message = exception.getMessage();
            document.setProcessingError(message == null ? "Không thể tạo embedding"
                    : message.substring(0, Math.min(message.length(), 2000)));
            documentRepository.save(document);
            return false;
        }
    }
}
