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
    private final DocumentChunker chunker;

    public void embed(KnowledgeDocument document) {
        List<KnowledgeVectorRepository.ChunkEmbedding> embeddings = chunker.split(document.getContent()).stream()
                .map(chunk -> new KnowledgeVectorRepository.ChunkEmbedding(chunk.index(), chunk.content(),
                        chunk.tokenEstimate(), geminiClient.embed(
                                document.getTitle() + "\n\n" + chunk.content(), "RETRIEVAL_DOCUMENT")))
                .toList();
        if (embeddings.isEmpty()) throw new IllegalArgumentException("Tài liệu không có nội dung để lập chỉ mục");
        vectorRepository.replaceChunks(document.getId(), document.getTitle(), embeddings);
    }

    public List<KnowledgeVectorRepository.SimilarityResult> search(
            String query, int limit, double threshold, boolean includeSafety) {
        return vectorRepository.search(query,
                geminiClient.embed(query, "RETRIEVAL_QUERY"), limit, threshold, includeSafety);
    }

    @Transactional
    public int reindexAll() {
        List<KnowledgeDocument> documents = documentRepository.findAll().stream()
                .filter(document -> Boolean.TRUE.equals(document.getActive()))
                .filter(document -> "APPROVED".equals(document.getReviewStatus()))
                .filter(document -> List.of("A", "B").contains(document.getSourceTier()))
                .filter(document -> document.getExpiresAt() == null || document.getExpiresAt().isAfter(Instant.now()))
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
        if (!"APPROVED".equals(document.getReviewStatus())
                || !List.of("A", "B").contains(document.getSourceTier())) {
            document.setActive(false);
            document.setProcessingStatus("PENDING_REVIEW");
            documentRepository.save(document);
            return false;
        }
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
