package com.mindcare.ai_service.dto;

import java.util.List;
import java.util.UUID;

public record RagChatResponse(String answer, List<Source> sources, SafetyDirective safety, UUID conversationId) {
    public RagChatResponse(String answer, List<Source> sources) {
        this(answer, sources, SafetyDirective.none(), null);
    }

    public RagChatResponse(String answer, List<Source> sources, SafetyDirective safety) {
        this(answer, sources, safety, null);
    }

    public record Source(int citationNumber, UUID id, String title, String sourceUrl, double similarity) {}

    public record SafetyDirective(
            String level,
            boolean showSafetyCheck,
            boolean showEmergencyActions,
            String emergencyNumber
    ) {
        public static SafetyDirective none() {
            return new SafetyDirective("NONE", false, false, null);
        }
    }
}
