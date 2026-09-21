package com.mindcare.ai_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mindcare.ai_service.entity.KnowledgeDocument;
import com.mindcare.ai_service.repository.KnowledgeDocumentRepository;
import com.mindcare.ai_service.repository.KnowledgeVectorRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EmbeddingServiceTest {
    @Test
    void reindexAllPersistsReadyAndFailedStatuses() {
        GeminiClient geminiClient = mock(GeminiClient.class);
        KnowledgeVectorRepository vectorRepository = mock(KnowledgeVectorRepository.class);
        KnowledgeDocumentRepository repository = mock(KnowledgeDocumentRepository.class);
        KnowledgeDocument successful = document("Stress");
        KnowledgeDocument failed = document("Sleep");
        when(repository.findAll()).thenReturn(List.of(successful, failed));
        when(repository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(geminiClient.embed(org.mockito.ArgumentMatchers.startsWith("Stress"),
                org.mockito.ArgumentMatchers.eq("RETRIEVAL_DOCUMENT"))).thenReturn(List.of(0.1));
        when(geminiClient.embed(org.mockito.ArgumentMatchers.startsWith("Sleep"),
                org.mockito.ArgumentMatchers.eq("RETRIEVAL_DOCUMENT")))
                .thenThrow(new RuntimeException("embedding unavailable"));
        DocumentChunker chunker = new DocumentChunker(500, 50);
        EmbeddingService service = new EmbeddingService(geminiClient, vectorRepository, repository, chunker);

        int count = service.reindexAll();

        assertThat(count).isEqualTo(1);
        assertThat(successful.getProcessingStatus()).isEqualTo("READY");
        assertThat(successful.getIndexedAt()).isNotNull();
        assertThat(failed.getProcessingStatus()).isEqualTo("FAILED");
        assertThat(failed.getProcessingError()).isEqualTo("embedding unavailable");
        verify(vectorRepository).replaceChunks(org.mockito.ArgumentMatchers.eq(successful.getId()),
                org.mockito.ArgumentMatchers.eq("Stress"), org.mockito.ArgumentMatchers.anyList());
    }

    private KnowledgeDocument document(String title) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(UUID.randomUUID());
        document.setTitle(title);
        document.setContent("Knowledge content");
        document.setActive(true);
        document.setReviewStatus("APPROVED");
        document.setSourceTier("A");
        return document;
    }
}
