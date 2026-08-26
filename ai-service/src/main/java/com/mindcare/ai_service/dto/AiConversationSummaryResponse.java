package com.mindcare.ai_service.dto;

import java.time.Instant;
import java.util.UUID;

public record AiConversationSummaryResponse(UUID id, String title, Instant createdAt, Instant updatedAt) {}
