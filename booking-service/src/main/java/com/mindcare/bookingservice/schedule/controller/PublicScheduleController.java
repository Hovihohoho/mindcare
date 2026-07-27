package com.mindcare.bookingservice.schedule.controller;

import com.mindcare.bookingservice.schedule.dto.ScheduleResponse;
import com.mindcare.bookingservice.schedule.service.ScheduleService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/experts/{expertUserId}/schedules")
@RequiredArgsConstructor
@Validated
public class PublicScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping
    public List<ScheduleResponse> listAvailable(
            @PathVariable UUID expertUserId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime to,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
        return scheduleService.listAvailable(
                expertUserId,
                from,
                to,
                limit);
    }
}
