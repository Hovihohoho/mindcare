package com.mindcare.ai_service.service;

import com.mindcare.ai_service.dto.AiConversationResponse;
import com.mindcare.ai_service.dto.AiConversationSummaryResponse;
import com.mindcare.ai_service.dto.RagChatRequest;
import com.mindcare.ai_service.dto.RagChatResponse;
import com.mindcare.ai_service.entity.AiConversation;
import com.mindcare.ai_service.entity.AiConversationMessage;
import com.mindcare.ai_service.exception.ConversationNotFoundException;
import com.mindcare.ai_service.repository.AiConversationMessageRepository;
import com.mindcare.ai_service.repository.AiConversationRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class AiConversationService {
    private final AiConversationRepository conversationRepository;
    private final AiConversationMessageRepository messageRepository;
    private final RagChatService ragChatService;
    private final ObjectMapper objectMapper;

    @Transactional
    public RagChatResponse chat(UUID userId, RagChatRequest request) {
        AiConversation conversation = request.conversationId() == null
                ? createConversation(userId, request.question())
                : ownedConversation(userId, request.conversationId());
        List<RagChatRequest.ConversationMessage> history = storedHistory(conversation.getId());
        saveMessage(conversation.getId(), "user", request.question(), null, "NONE");

        RagChatResponse generated = ragChatService.chat(new RagChatRequest(
                request.question(), request.topK(), history, conversation.getId()));
        saveMessage(
                conversation.getId(),
                "assistant",
                generated.answer(),
                writeSources(generated.sources()),
                generated.safety().level());
        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);
        // The caller inserts a JDBC replay record referencing this conversation.
        // Flush pending JPA inserts before that foreign-key check, in the same transaction.
        conversationRepository.flush();
        return new RagChatResponse(
                generated.answer(), generated.sources(), generated.safety(), conversation.getId());
    }

    @Transactional(readOnly = true)
    public List<AiConversationSummaryResponse> list(UUID userId) {
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(item -> new AiConversationSummaryResponse(
                        item.getId(), item.getTitle(), item.getCreatedAt(), item.getUpdatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public AiConversationResponse get(UUID userId, UUID conversationId) {
        AiConversation conversation = ownedConversation(userId, conversationId);
        List<AiConversationResponse.Message> messages = messageRepository
                .findByConversationIdOrderByCreatedAtAscIdAsc(conversationId).stream()
                .map(this::toResponse)
                .toList();
        return new AiConversationResponse(
                conversation.getId(), conversation.getTitle(), conversation.getCreatedAt(),
                conversation.getUpdatedAt(), messages);
    }

    @Transactional
    public AiConversationSummaryResponse rename(UUID userId, UUID conversationId, String title) {
        AiConversation conversation = ownedConversation(userId, conversationId);
        conversation.setTitle(title.trim());
        conversation.setUpdatedAt(Instant.now());
        AiConversation saved = conversationRepository.save(conversation);
        return new AiConversationSummaryResponse(
                saved.getId(), saved.getTitle(), saved.getCreatedAt(), saved.getUpdatedAt());
    }

    @Transactional
    public void delete(UUID userId, UUID conversationId) {
        conversationRepository.delete(ownedConversation(userId, conversationId));
    }

    @Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public List<AiConversationResponse> exportAll(UUID userId) {
        List<AiConversationResponse> exported = new ArrayList<>();
        org.springframework.data.domain.Pageable page = org.springframework.data.domain.PageRequest.of(0, 50);
        org.springframework.data.domain.Slice<AiConversation> batch;
        do {
            batch = conversationRepository.findByUserIdOrderByIdAsc(userId, page);
            batch.forEach(item -> exported.add(get(userId, item.getId())));
            page = batch.nextPageable();
        } while (batch.hasNext());
        return exported;
    }

    @Transactional
    public long deleteAll(UUID userId) {
        return conversationRepository.deleteByUserId(userId);
    }

    private AiConversation createConversation(UUID userId, String question) {
        AiConversation conversation = new AiConversation();
        conversation.setUserId(userId);
        conversation.setTitle(defaultTitle(question));
        return conversationRepository.save(conversation);
    }

    private AiConversation ownedConversation(UUID userId, UUID conversationId) {
        return conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(ConversationNotFoundException::new);
    }

    private List<RagChatRequest.ConversationMessage> storedHistory(UUID conversationId) {
        List<AiConversationMessage> latest = new ArrayList<>(
                messageRepository.findTop6ByConversationIdOrderByCreatedAtDescIdDesc(conversationId));
        Collections.reverse(latest);
        return latest.stream()
                .map(item -> new RagChatRequest.ConversationMessage(item.getRole(), item.getContent()))
                .toList();
    }

    private void saveMessage(
            UUID conversationId, String role, String content, String sourcesJson, String safetyLevel) {
        AiConversationMessage message = new AiConversationMessage();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        message.setSourcesJson(sourcesJson);
        message.setSafetyLevel(safetyLevel);
        messageRepository.save(message);
    }

    private AiConversationResponse.Message toResponse(AiConversationMessage message) {
        return new AiConversationResponse.Message(
                message.getId(), message.getRole(), message.getContent(), readSources(message.getSourcesJson()),
                message.getSafetyLevel(), message.getCreatedAt());
    }

    private String writeSources(List<RagChatResponse.Source> sources) {
        try {
            return sources == null || sources.isEmpty() ? null : objectMapper.writeValueAsString(sources);
        } catch (RuntimeException error) {
            throw new IllegalStateException("Không thể lưu nguồn của phản hồi AI.", error);
        }
    }

    private List<RagChatResponse.Source> readSources(String value) {
        if (value == null || value.isBlank()) return List.of();
        try {
            return List.of(objectMapper.readValue(value, RagChatResponse.Source[].class));
        } catch (RuntimeException error) {
            return List.of();
        }
    }

    private String defaultTitle(String question) {
        String normalized = question.trim().replaceAll("\\s+", " ");
        int[] codePoints = normalized.codePoints().limit(60).toArray();
        String title = new String(codePoints, 0, codePoints.length);
        return normalized.codePointCount(0, normalized.length()) > 60 ? title + "…" : title;
    }
}
