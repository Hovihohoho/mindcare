package com.mindcare.emotionservice.assessment.controller;

import com.mindcare.emotionservice.assessment.dto.AssessmentSummaryResponse;
import com.mindcare.emotionservice.assessment.dto.AssessmentDetailResponse;
import com.mindcare.emotionservice.assessment.dto.AssessmentResultResponse;
import com.mindcare.emotionservice.assessment.dto.AssessmentSubmissionRequest;
import com.mindcare.emotionservice.assessment.entity.AssessmentCode;
import com.mindcare.emotionservice.assessment.service.AssessmentService;
import com.mindcare.emotionservice.shared.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(
        path = "/api/v1/assessments",
        produces = MediaType.APPLICATION_JSON_VALUE
)
public class AssessmentController {

    private final AssessmentService assessmentService;

    public AssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @GetMapping
    public List<AssessmentSummaryResponse> getPublishedAssessments() {
        return assessmentService.getPublishedAssessments();
    }

    @GetMapping("/{assessmentCode}")
    public AssessmentDetailResponse getPublishedAssessment(
            @PathVariable AssessmentCode assessmentCode
    ) {
        return assessmentService.getPublishedAssessment(assessmentCode);
    }

    @PostMapping(
            path = "/{assessmentCode}/submissions",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AssessmentResultResponse> submitAssessment(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable AssessmentCode assessmentCode,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody AssessmentSubmissionRequest request
    ) {
        AssessmentResultResponse response = assessmentService.submitAssessment(
                authenticatedUser.userId(),
                assessmentCode,
                idempotencyKey,
                request
        );
        URI location = URI.create("/api/v1/assessment-results/" + response.resultId());
        return ResponseEntity.created(location).body(response);
    }
}
