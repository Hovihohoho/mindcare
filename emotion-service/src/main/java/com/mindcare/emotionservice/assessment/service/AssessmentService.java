package com.mindcare.emotionservice.assessment.service;

import com.mindcare.emotionservice.assessment.dto.AdminAssessmentResponse;
import com.mindcare.emotionservice.assessment.dto.AssessmentDetailResponse;
import com.mindcare.emotionservice.assessment.dto.AssessmentResultResponse;
import com.mindcare.emotionservice.assessment.dto.AssessmentSubmissionRequest;
import com.mindcare.emotionservice.assessment.dto.AssessmentSummaryResponse;
import com.mindcare.emotionservice.assessment.dto.UpsertAssessmentRequest;
import com.mindcare.emotionservice.assessment.entity.AssessmentCode;
import com.mindcare.emotionservice.shared.dto.CursorPageResponse;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AssessmentService {

    List<AssessmentSummaryResponse> getPublishedAssessments();

    AssessmentDetailResponse getPublishedAssessment(AssessmentCode assessmentCode);

    AssessmentResultResponse submitAssessment(
            UUID userId,
            AssessmentCode assessmentCode,
            String idempotencyKey,
            AssessmentSubmissionRequest request
    );

    CursorPageResponse<AssessmentResultResponse> getAssessmentHistory(
            UUID userId,
            OffsetDateTime from,
            OffsetDateTime to,
            String cursor,
            int limit
    );

    AssessmentResultResponse getAssessmentResult(UUID userId, UUID resultId);

    AdminAssessmentResponse createAssessment(UpsertAssessmentRequest request);

    AdminAssessmentResponse updateAssessment(
            UUID assessmentId,
            UpsertAssessmentRequest request
    );

    AdminAssessmentResponse createNextAssessmentVersion(UUID assessmentId);

    AdminAssessmentResponse publishAssessment(UUID assessmentId);

    AdminAssessmentResponse archiveAssessment(UUID assessmentId);

    AdminAssessmentResponse getAssessmentForAdmin(UUID assessmentId);

    CursorPageResponse<AdminAssessmentResponse> getAssessmentsForAdmin(
            String status,
            String cursor,
            int limit
    );
}
