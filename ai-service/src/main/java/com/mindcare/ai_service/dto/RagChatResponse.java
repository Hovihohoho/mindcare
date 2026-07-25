package com.mindcare.ai_service.dto;

import java.util.List;
import java.util.UUID;

public record RagChatResponse(String answer, List<Source> sources) {
    public record Source(UUID id, String title, String sourceUrl, double similarity) {}
}
