package com.mindcare.ai_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameAiConversationRequest(@NotBlank @Size(max = 120) String title) {}
