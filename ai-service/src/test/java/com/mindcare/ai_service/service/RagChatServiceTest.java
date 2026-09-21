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
        assertThat(response.safety().level()).isEqualTo("NONE");
        assertThat(response.safety().showEmergencyActions()).isFalse();
    }

    @Test
    void explicitSelfHarmSignalReturnsStructuredEmergencyActionsEvenWithoutContext() {
        EmbeddingService embeddings = mock(EmbeddingService.class);
        GeminiClient gemini = mock(GeminiClient.class);
        String question = "I want to kill myself";
        when(embeddings.search(question, 5, 0.35, true)).thenReturn(List.of());
        RagChatService service = configuredService(embeddings, gemini);

        var response = service.chat(new RagChatRequest(question, null));

        assertThat(response.safety().level()).isEqualTo("EXPLICIT");
        assertThat(response.safety().showSafetyCheck()).isTrue();
        assertThat(response.safety().showEmergencyActions()).isTrue();
        assertThat(response.safety().emergencyNumber()).isEqualTo("115");
    }

    @Test
    void ambiguousDistressRequestsSafetyCheckWithoutEmergencyAction() {
        EmbeddingService embeddings = mock(EmbeddingService.class);
        GeminiClient gemini = mock(GeminiClient.class);
        String question = "Tôi thấy tuyệt vọng";
        when(embeddings.search(question, 5, 0.35, true)).thenReturn(List.of());
        RagChatService service = configuredService(embeddings, gemini);

        var response = service.chat(new RagChatRequest(question, null));

        assertThat(response.safety().level()).isEqualTo("CHECK_IN");
        assertThat(response.safety().showSafetyCheck()).isTrue();
        assertThat(response.safety().showEmergencyActions()).isFalse();
    }

    private RagChatService configuredService(EmbeddingService embeddings, GeminiClient gemini) {
        RagChatService service = new RagChatService(embeddings, gemini, new CrisisRiskDetector());
        ReflectionTestUtils.setField(service, "defaultTopK", 5);
        ReflectionTestUtils.setField(service, "maxTopK", 10);
        ReflectionTestUtils.setField(service, "threshold", 0.35);
        return service;
    }

    @Test
    void embeddingFailureStillReturnsSafetyMetadata() {
        EmbeddingService embeddings = mock(EmbeddingService.class);
        GeminiClient gemini = mock(GeminiClient.class);
        String question = "Tôi muốn chết ngay bây giờ";
        when(embeddings.search(question, 5, 0.35, true))
                .thenThrow(new IllegalStateException("provider error containing sensitive content"));
        var response = configuredService(embeddings, gemini).chat(new RagChatRequest(question, 5));
        assertThat(response.safety().level()).isEqualTo("IMMINENT");
        assertThat(response.safety().showEmergencyActions()).isTrue();
        assertThat(response.answer()).doesNotContain("sensitive");
        org.mockito.Mockito.verifyNoInteractions(gemini);
    }

    @Test
    void generationTimeoutStillReturnsSafetyMetadata() {
        EmbeddingService embeddings = mock(EmbeddingService.class);
        GeminiClient gemini = mock(GeminiClient.class);
        String question = "Tôi đang nghĩ đến việc tự sát";
        when(embeddings.search(question, 5, 0.35, true)).thenReturn(List.of(
                new SimilarityResult(UUID.randomUUID(), UUID.randomUUID(), 0, "Safety", "Support",
                        "https://www.nimh.nih.gov/", 0.8)));
        when(gemini.generate(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .thenThrow(new org.springframework.web.client.ResourceAccessException("timeout"));
        var response = configuredService(embeddings, gemini).chat(new RagChatRequest(question, 5));
        assertThat(response.safety().level()).isEqualTo("EXPLICIT");
        assertThat(response.sources()).isEmpty();
    }

    @Test
    void invalidCitationsAreRepairedOnceThenRefused() {
        EmbeddingService embeddings = mock(EmbeddingService.class);
        GeminiClient gemini = mock(GeminiClient.class);
        when(embeddings.search("Stress", 5, 0.35, false)).thenReturn(List.of(
                new SimilarityResult(UUID.randomUUID(), UUID.randomUUID(), 0, "Stress", "Support",
                        "https://www.who.int/", 0.8)));
        when(gemini.generate(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("Unsupported [Nguồn 99]");
        var response = configuredService(embeddings, gemini).chat(new RagChatRequest("Stress", 5));
        assertThat(response.sources()).isEmpty();
        assertThat(response.answer()).contains("chưa thể");
        verify(gemini, org.mockito.Mockito.times(2)).generate(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void groupedCitationSyntaxIsRepairedInsteadOfSilentlyAccepted() {
        EmbeddingService embeddings = mock(EmbeddingService.class);
        GeminiClient gemini = mock(GeminiClient.class);
        when(embeddings.search("Stress", 5, 0.35, false)).thenReturn(List.of(
                new SimilarityResult(UUID.randomUUID(), UUID.randomUUID(), 0, "Stress", "Support",
                        "https://www.who.int/", 0.8),
                new SimilarityResult(UUID.randomUUID(), UUID.randomUUID(), 1, "Support", "More support",
                        "https://www.nih.gov/", 0.75)));
        when(gemini.generate(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn("Nhận định [Nguồn 1, Nguồn 2]", "Nhận định [Nguồn 1] [Nguồn 2]");
        var response = configuredService(embeddings, gemini).chat(new RagChatRequest("Stress", 5));
        assertThat(response.answer()).isEqualTo("Nhận định [Nguồn 1] [Nguồn 2]");
        assertThat(response.sources()).hasSize(2);
        verify(gemini, org.mockito.Mockito.times(2)).generate(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }
}
