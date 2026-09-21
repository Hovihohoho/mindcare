package com.mindcare.ai_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mindcare.ai_service.dto.RagChatRequest;
import com.mindcare.ai_service.dto.RagChatResponse;
import com.mindcare.ai_service.entity.AiConversation;
import com.mindcare.ai_service.exception.ConversationNotFoundException;
import com.mindcare.ai_service.repository.AiConversationMessageRepository;
import com.mindcare.ai_service.repository.AiConversationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class AiConversationServiceTest {
    private AiConversationRepository conversations;
    private AiConversationMessageRepository messages;
    private RagChatService ragChatService;
    private AiConversationService service;

    @BeforeEach
    void setUp() {
        conversations = mock(AiConversationRepository.class);
        messages = mock(AiConversationMessageRepository.class);
        ragChatService = mock(RagChatService.class);
        service = new AiConversationService(conversations, messages, ragChatService, new ObjectMapper());
    }

    @Test
    void newChatCreatesOwnedConversationAndStoresBothMessages() {
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        when(conversations.save(any(AiConversation.class))).thenAnswer(invocation -> {
            AiConversation conversation = invocation.getArgument(0);
            if (conversation.getId() == null) conversation.setId(conversationId);
            if (conversation.getCreatedAt() == null) conversation.setCreatedAt(Instant.now());
            return conversation;
        });
        when(messages.findTop6ByConversationIdOrderByCreatedAtDescIdDesc(conversationId))
                .thenReturn(List.of());
        RagChatResponse generated = new RagChatResponse(
                "Mình đang lắng nghe.",
                List.of(),
                RagChatResponse.SafetyDirective.none());
        when(ragChatService.chat(any(RagChatRequest.class))).thenReturn(generated);

        RagChatResponse response = service.chat(userId, new RagChatRequest("Một ngày khó khăn", 5));

        assertThat(response.conversationId()).isEqualTo(conversationId);
        assertThat(response.answer()).isEqualTo("Mình đang lắng nghe.");
        verify(messages, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void anotherUsersConversationIsReportedAsNotFound() {
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        when(conversations.findByIdAndUserId(conversationId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(userId, conversationId))
                .isInstanceOf(ConversationNotFoundException.class);
    }

    @Test
    void deleteRequiresOwnershipBeforeDeleting() {
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        AiConversation conversation = new AiConversation();
        conversation.setId(conversationId);
        conversation.setUserId(userId);
        when(conversations.findByIdAndUserId(conversationId, userId))
                .thenReturn(Optional.of(conversation));

        service.delete(userId, conversationId);

        verify(conversations).delete(conversation);
    }

    @Test
    void exportIncludesEveryPageAndKeepsOwnership() {
        UUID user = UUID.randomUUID();
        var all = java.util.stream.IntStream.range(0, 53).mapToObj(index -> {
            AiConversation conversation = new AiConversation();
            conversation.setId(UUID.randomUUID());
            conversation.setUserId(user);
            when(conversations.findByIdAndUserId(conversation.getId(), user)).thenReturn(Optional.of(conversation));
            when(messages.findByConversationIdOrderByCreatedAtAscIdAsc(conversation.getId())).thenReturn(List.of());
            return conversation;
        }).toList();
        var first = org.springframework.data.domain.PageRequest.of(0, 50);
        var second = org.springframework.data.domain.PageRequest.of(1, 50);
        when(conversations.findByUserIdOrderByIdAsc(user, first))
                .thenReturn(new org.springframework.data.domain.SliceImpl<>(all.subList(0, 50), first, true));
        when(conversations.findByUserIdOrderByIdAsc(user, second))
                .thenReturn(new org.springframework.data.domain.SliceImpl<>(all.subList(50, 53), second, false));

        assertThat(service.exportAll(user)).hasSize(53)
                .extracting(item -> item.id()).containsExactlyElementsOf(all.stream().map(AiConversation::getId).toList());
    }

    @Test
    void deleteAllIsIdempotentAtRepositoryBoundary() {
        UUID userId = UUID.randomUUID();
        when(conversations.deleteByUserId(userId)).thenReturn(0L);

        assertThat(service.deleteAll(userId)).isZero();
        verify(conversations).deleteByUserId(userId);
    }
}
