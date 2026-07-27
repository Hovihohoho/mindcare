package com.mindcare.bookingservice.consultation.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ConsultationNoteResponse(
        UUID id,
        UUID bookingId,
        UUID expertUserId,
        String observation,
        String recommendation,
        String recoveryPlan,
        boolean visibleToUser,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
