package com.mindcare.bookingservice.consultation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertConsultationNoteRequest(
        @Size(max = 5000) String observation,
        @NotBlank @Size(max = 5000) String recommendation,
        @Size(max = 5000) String recoveryPlan,
        boolean visibleToUser) {
}
