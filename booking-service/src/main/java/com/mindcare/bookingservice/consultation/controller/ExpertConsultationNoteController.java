package com.mindcare.bookingservice.consultation.controller;

import com.mindcare.bookingservice.consultation.dto.ConsultationNoteResponse;
import com.mindcare.bookingservice.consultation.dto.UpsertConsultationNoteRequest;
import com.mindcare.bookingservice.consultation.service.ConsultationNoteService;
import com.mindcare.bookingservice.shared.security.CurrentUserProvider;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/expert/bookings/{bookingId}/consultation-note")
@PreAuthorize("hasRole('EXPERT')")
@RequiredArgsConstructor
public class ExpertConsultationNoteController {

    private final ConsultationNoteService consultationNoteService;
    private final CurrentUserProvider currentUserProvider;

    @PutMapping
    public ConsultationNoteResponse upsert(
            @PathVariable UUID bookingId,
            @Valid @RequestBody UpsertConsultationNoteRequest request) {
        return consultationNoteService.upsert(
                currentUserProvider.getRequiredUserId(),
                bookingId,
                request);
    }

    @GetMapping
    public ConsultationNoteResponse get(@PathVariable UUID bookingId) {
        return consultationNoteService.getForExpert(
                currentUserProvider.getRequiredUserId(),
                bookingId);
    }
}
