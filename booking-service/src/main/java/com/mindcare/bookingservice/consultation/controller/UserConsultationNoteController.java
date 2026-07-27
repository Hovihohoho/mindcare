package com.mindcare.bookingservice.consultation.controller;

import com.mindcare.bookingservice.consultation.dto.UserConsultationNoteResponse;
import com.mindcare.bookingservice.consultation.service.ConsultationNoteService;
import com.mindcare.bookingservice.shared.security.CurrentUserProvider;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings/{bookingId}/consultation-note")
@PreAuthorize("hasRole('USER')")
@RequiredArgsConstructor
public class UserConsultationNoteController {

    private final ConsultationNoteService consultationNoteService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public UserConsultationNoteResponse get(@PathVariable UUID bookingId) {
        return consultationNoteService.getForUser(
                currentUserProvider.getRequiredUserId(),
                bookingId);
    }
}
