package com.mindcare.ai_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mindcare.ai_service.dto.RagChatRequest;
import com.mindcare.ai_service.repository.KnowledgeVectorRepository.SimilarityResult;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RagChatServiceTest {
    @Test
    void insomniaDoesNotRetrieveSafetyDocumentsOrTriggerCrisisPrompt() {
        EmbeddingService embeddings = mock(EmbeddingService.class);
        GeminiClient gemini = mock(GeminiClient.class);
        String question = "Khó ngủ, trằn trọc đến 3 giờ sáng mới ngủ được";
        SimilarityResult sleep = new SimilarityResult(UUID.randomUUID(), UUID.randomUUID(), 0,
                "Giấc ngủ và sức khỏe tinh thần", "Duy trì giờ thức dậy tương đối ổn định.",
                "https://www.who.int/", 0.82);
        when(embeddings.search(question, 5, 0.35, false)).thenReturn(List.of(sleep));
        when(gemini.generate(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("Tình trạng này đã kéo dài bao lâu rồi? [Nguồn 1]");
        RagChatService service = new RagChatService(embeddings, gemini, new CrisisRiskDetector());
        ReflectionTestUtils.setField(service, "defaultTopK", 5);
        ReflectionTestUtils.setField(service, "maxTopK", 10);
        ReflectionTestUtils.setField(service, "threshold", 0.35);

        var response = service.chat(new RagChatRequest(question, null));

        verify(embeddings).search(question, 5, 0.35, false);
        assertThat(response.answer()).doesNotContainIgnoringCase("tự hại", "tự sát", "cấp cứu");
        assertThat(response.sources()).singleElement().extracting(source -> source.title())
                .isEqualTo("Giấc ngủ và sức khỏe tinh thần");
    }
}
