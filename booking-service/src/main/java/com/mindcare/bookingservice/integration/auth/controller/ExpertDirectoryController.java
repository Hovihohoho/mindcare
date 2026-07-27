package com.mindcare.bookingservice.integration.auth.controller;

import com.mindcare.bookingservice.integration.auth.dto.ExpertSummaryResponse;
import com.mindcare.bookingservice.integration.auth.service.ExpertDirectoryService;
import com.mindcare.bookingservice.shared.dto.CursorPageResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/experts")
@PreAuthorize("hasRole('USER')")
@RequiredArgsConstructor
@Validated
public class ExpertDirectoryController {

    private final ExpertDirectoryService expertDirectoryService;

    @GetMapping
    public CursorPageResponse<ExpertSummaryResponse> listBookable(
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(required = false) @Size(max = 100) String specialty,
            @RequestParam(required = false) @Size(max = 512) String cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return expertDirectoryService.listBookable(
                keyword,
                specialty,
                cursor,
                limit);
    }
}
