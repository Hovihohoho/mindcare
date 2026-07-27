package com.mindcare.emotionservice.assessment.controller;

import com.mindcare.emotionservice.assessment.dto.AssessmentResultResponse;
import com.mindcare.emotionservice.assessment.service.AssessmentService;
import com.mindcare.emotionservice.shared.dto.CursorPageResponse;
import com.mindcare.emotionservice.shared.security.AuthenticatedUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping(
        path = "/api/v1/assessment-results",
        produces = MediaType.APPLICATION_JSON_VALUE
)
public class AssessmentResultController {

    private final AssessmentService assessmentService;

    public AssessmentResultController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @GetMapping
    public CursorPageResponse<AssessmentResultResponse> getAssessmentHistory(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime to,
            @RequestParam(required = false)
            String cursor,
            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(100)
            int limit
    ) {
        return assessmentService.getAssessmentHistory(
                authenticatedUser.userId(),
                from,
                to,
                cursor,
                limit
        );
    }

    @GetMapping("/{resultId}")
    public AssessmentResultResponse getAssessmentResult(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable UUID resultId
    ) {
        return assessmentService.getAssessmentResult(authenticatedUser.userId(), resultId);
    }
}
