package com.mindcare.ai_service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.UUID;

public record RagChatRequest(
        @NotBlank @Size(max = 4000) String question,
        @Min(1) @Max(10) Integer topK,
        @Size(max = 10) List<@Valid ConversationMessage> history,
        UUID conversationId) {

    public RagChatRequest(String question, Integer topK) {
        this(question, topK, List.of(), null);
    }

    public RagChatRequest(String question, Integer topK, List<ConversationMessage> history) {
        this(question, topK, history, null);
    }

    public record ConversationMessage(
            @NotBlank @Pattern(regexp = "user|assistant") String role,
            @NotBlank @Size(max = 2000) String content) {}
}
