package com.mindcare.ai_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KnowledgeDocumentRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank String content,
        @Size(max = 1000) String sourceUrl,
        @Size(max = 50) String documentType,
        Boolean active) {
}
