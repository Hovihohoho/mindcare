package com.mindcare.emotionservice.assessment.controller;

import com.mindcare.emotionservice.assessment.dto.AdminAssessmentResponse;
import com.mindcare.emotionservice.assessment.dto.UpsertAssessmentRequest;
import com.mindcare.emotionservice.assessment.service.AssessmentService;
import com.mindcare.emotionservice.shared.dto.CursorPageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping(
        path = "/api/v1/admin/assessments",
        produces = MediaType.APPLICATION_JSON_VALUE
)
public class AdminAssessmentController {

    private final AssessmentService assessmentService;

    public AdminAssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @GetMapping
    public CursorPageResponse<AdminAssessmentResponse> getAssessments(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(100)
            int limit
    ) {
        return assessmentService.getAssessmentsForAdmin(status, cursor, limit);
    }

    @GetMapping("/{assessmentId}")
    public AdminAssessmentResponse getAssessment(
            @PathVariable UUID assessmentId
    ) {
        return assessmentService.getAssessmentForAdmin(assessmentId);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AdminAssessmentResponse> createAssessment(
            @Valid @RequestBody UpsertAssessmentRequest request
    ) {
        AdminAssessmentResponse response = assessmentService.createAssessment(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/admin/assessments/" + response.id()))
                .body(response);
    }

    @PutMapping(
            path = "/{assessmentId}",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public AdminAssessmentResponse updateAssessmentDraft(
            @PathVariable UUID assessmentId,
            @Valid @RequestBody UpsertAssessmentRequest request
    ) {
        return assessmentService.updateAssessment(assessmentId, request);
    }

    @PostMapping("/{assessmentId}:create-next-version")
    public ResponseEntity<AdminAssessmentResponse> createNextAssessmentVersion(
            @PathVariable UUID assessmentId
    ) {
        AdminAssessmentResponse response = assessmentService.createNextAssessmentVersion(assessmentId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .location(URI.create("/api/v1/admin/assessments/" + response.id()))
                .body(response);
    }

    @PostMapping("/{assessmentId}:publish")
    public AdminAssessmentResponse publishAssessment(
            @PathVariable UUID assessmentId
    ) {
        return assessmentService.publishAssessment(assessmentId);
    }

    @PostMapping("/{assessmentId}:archive")
    public AdminAssessmentResponse archiveAssessment(
            @PathVariable UUID assessmentId
    ) {
        return assessmentService.archiveAssessment(assessmentId);
    }
}
