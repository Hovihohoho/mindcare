package com.mindcare.ai_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record KnowledgeDocumentRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank String content,
        @NotBlank @Size(max = 1000)
        @Pattern(regexp = "https://.+", message = "sourceUrl must use HTTPS") String sourceUrl,
        @Size(max = 50) String documentType,
        Boolean active) {
}
