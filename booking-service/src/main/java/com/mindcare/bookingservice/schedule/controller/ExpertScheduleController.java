package com.mindcare.bookingservice.schedule.controller;

import com.mindcare.bookingservice.schedule.dto.CreateScheduleRequest;
import com.mindcare.bookingservice.schedule.dto.ScheduleResponse;
import com.mindcare.bookingservice.schedule.dto.UpdateScheduleRequest;
import com.mindcare.bookingservice.schedule.service.ScheduleService;
import com.mindcare.bookingservice.shared.security.CurrentUserProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/expert/schedules")
@PreAuthorize("hasRole('EXPERT')")
@RequiredArgsConstructor
@Validated
public class ExpertScheduleController {

    private final ScheduleService scheduleService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    public ResponseEntity<ScheduleResponse> create(
            @Valid @RequestBody CreateScheduleRequest request) {
        ScheduleResponse response =
                scheduleService.create(currentUserProvider.getRequiredUserId(), request);
        return ResponseEntity
                .created(URI.create("/api/v1/expert/schedules/" + response.id()))
                .body(response);
    }

    @GetMapping
    public List<ScheduleResponse> listOwn(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime to,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
        return scheduleService.listOwn(
                currentUserProvider.getRequiredUserId(),
                from,
                to,
                limit);
    }

    @PutMapping("/{scheduleId}")
    public ScheduleResponse update(
            @PathVariable UUID scheduleId,
            @Valid @RequestBody UpdateScheduleRequest request) {
        return scheduleService.update(
                currentUserProvider.getRequiredUserId(),
                scheduleId,
                request);
    }

    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> cancel(@PathVariable UUID scheduleId) {
        scheduleService.cancel(
                currentUserProvider.getRequiredUserId(),
                scheduleId);
        return ResponseEntity.noContent().build();
    }
}
