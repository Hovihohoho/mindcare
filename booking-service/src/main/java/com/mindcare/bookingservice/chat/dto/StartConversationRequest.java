package com.mindcare.bookingservice.chat.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StartConversationRequest(@NotNull UUID expertUserId) {
}
