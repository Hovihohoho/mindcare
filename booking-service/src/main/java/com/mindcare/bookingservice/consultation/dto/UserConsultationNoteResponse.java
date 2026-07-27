package com.mindcare.bookingservice.consultation.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserConsultationNoteResponse(
        UUID id,
        UUID bookingId,
        String recommendation,
        String recoveryPlan,
        OffsetDateTime updatedAt) {
}
