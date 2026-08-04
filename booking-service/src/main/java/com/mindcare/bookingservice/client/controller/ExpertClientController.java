package com.mindcare.bookingservice.client.controller;

import com.mindcare.bookingservice.client.dto.ExpertClientProfileResponse;
import com.mindcare.bookingservice.client.service.ExpertClientService;
import com.mindcare.bookingservice.shared.security.CurrentUserProvider;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/expert/clients")
@PreAuthorize("hasRole('EXPERT')")
@RequiredArgsConstructor
public class ExpertClientController {

    private final ExpertClientService expertClientService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/by-schedule/{scheduleId}")
    public ExpertClientProfileResponse getBySchedule(@PathVariable UUID scheduleId) {
        return expertClientService.getBySchedule(
                currentUserProvider.getRequiredUserId(),
                scheduleId);
    }

    @GetMapping("/{userId}")
    public ExpertClientProfileResponse getByUser(@PathVariable UUID userId) {
        return expertClientService.getByUser(
                currentUserProvider.getRequiredUserId(),
                userId);
    }
}
