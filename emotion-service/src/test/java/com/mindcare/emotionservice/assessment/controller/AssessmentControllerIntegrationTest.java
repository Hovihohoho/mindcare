package com.mindcare.emotionservice.assessment.controller;

import com.mindcare.emotionservice.assessment.dto.AssessmentSummaryResponse;
import com.mindcare.emotionservice.assessment.dto.AnswerOptionResponse;
import com.mindcare.emotionservice.assessment.dto.AssessmentDetailResponse;
import com.mindcare.emotionservice.assessment.dto.AssessmentAnswerRequest;
import com.mindcare.emotionservice.assessment.dto.AssessmentResultResponse;
import com.mindcare.emotionservice.assessment.dto.AssessmentSubmissionRequest;
import com.mindcare.emotionservice.assessment.dto.QuestionResponse;
import com.mindcare.emotionservice.assessment.entity.AssessmentCode;
import com.mindcare.emotionservice.assessment.service.AssessmentService;
import com.mindcare.emotionservice.shared.exception.ResourceConflictException;
import com.mindcare.emotionservice.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AssessmentControllerIntegrationTest {

    private static final String ENDPOINT = "/api/v1/assessments";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssessmentService assessmentService;

    @Test
    void userCanListPublishedAssessments() throws Exception {
        UUID gad7Id = UUID.randomUUID();
        UUID phq9Id = UUID.randomUUID();
        when(assessmentService.getPublishedAssessments()).thenReturn(List.of(
                new AssessmentSummaryResponse(
                        gad7Id,
                        AssessmentCode.GAD_7,
                        1,
                        "GAD-7",
                        "Anxiety screening"
                ),
                new AssessmentSummaryResponse(
                        phq9Id,
                        AssessmentCode.PHQ_9,
                        2,
                        "PHQ-9",
                        "Mood screening"
                )
        ));

        mockMvc.perform(get(ENDPOINT)
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(gad7Id.toString()))
                .andExpect(jsonPath("$[0].code").value("GAD-7"))
                .andExpect(jsonPath("$[0].assessmentVersion").value(1))
                .andExpect(jsonPath("$[1].id").value(phq9Id.toString()))
                .andExpect(jsonPath("$[1].code").value("PHQ-9"))
                .andExpect(jsonPath("$[1].assessmentVersion").value(2))
                .andExpect(jsonPath("$[0].questions").doesNotExist())
                .andExpect(jsonPath("$[0].status").doesNotExist());

        verify(assessmentService).getPublishedAssessments();
    }

    @Test
    void userReceivesEmptyArrayWhenNoAssessmentIsPublished() throws Exception {
        when(assessmentService.getPublishedAssessments()).thenReturn(List.of());

        mockMvc.perform(get(ENDPOINT)
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(assessmentService).getPublishedAssessments();
    }

    @Test
    void userCanGetPublishedAssessmentContentWithoutScores() throws Exception {
        UUID assessmentId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID optionId = UUID.randomUUID();
        when(assessmentService.getPublishedAssessment(AssessmentCode.PHQ_9))
                .thenReturn(new AssessmentDetailResponse(
                        assessmentId,
                        AssessmentCode.PHQ_9,
                        2,
                        "PHQ-9",
                        "Mood screening",
                        List.of(new QuestionResponse(
                                questionId,
                                "Question 1",
                                0,
                                List.of(new AnswerOptionResponse(optionId, "Không hề"))
                        ))
                ));

        mockMvc.perform(get(ENDPOINT + "/PHQ-9")
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(assessmentId.toString()))
                .andExpect(jsonPath("$.code").value("PHQ-9"))
                .andExpect(jsonPath("$.assessmentVersion").value(2))
                .andExpect(jsonPath("$.questions[0].id").value(questionId.toString()))
                .andExpect(jsonPath("$.questions[0].orderIndex").value(0))
                .andExpect(jsonPath("$.questions[0].answerOptions[0].id").value(optionId.toString()))
                .andExpect(jsonPath("$.questions[0].answerOptions[0].optionText").value("Không hề"))
                .andExpect(jsonPath("$.questions[0].answerOptions[0].scoreValue").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist());

        verify(assessmentService).getPublishedAssessment(AssessmentCode.PHQ_9);
    }

    @Test
    void publishedAssessmentDetailReturnsNotFoundWhenCodeHasNoPublishedVersion() throws Exception {
        when(assessmentService.getPublishedAssessment(AssessmentCode.WHO_5))
                .thenThrow(new ResourceNotFoundException("Published assessment"));

        mockMvc.perform(get(ENDPOINT + "/WHO-5")
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_USER"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void invalidAssessmentCodeReturnsMalformedRequestBeforeServiceCall() throws Exception {
        mockMvc.perform(get(ENDPOINT + "/CUSTOM")
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_USER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        verifyNoInteractions(assessmentService);
    }

    @Test
    void nonUserRoleCannotReadPublishedAssessmentDetail() throws Exception {
        mockMvc.perform(get(ENDPOINT + "/PHQ-9")
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_EXPERT"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        verifyNoInteractions(assessmentService);
    }

    @Test
    void missingIdentityCannotListPublishedAssessments() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .header(USER_ROLE_HEADER, "ROLE_USER"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        verifyNoInteractions(assessmentService);
    }

    @Test
    void nonUserRoleCannotListPublishedAssessments() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_ADMIN"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        verifyNoInteractions(assessmentService);
    }

    @Test
    void userCanSubmitPublishedAssessmentAndReceiveScoredResult() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID optionId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();
        AssessmentResultResponse result = new AssessmentResultResponse(
                resultId,
                AssessmentCode.PHQ_9,
                1,
                8,
                "MILD",
                "Kết quả này chỉ có mục đích sàng lọc, không phải chẩn đoán.",
                List.of("Theo dõi cảm xúc của bạn."),
                OffsetDateTime.parse("2026-07-25T10:00:00Z")
        );
        when(assessmentService.submitAssessment(
                eq(userId),
                eq(AssessmentCode.PHQ_9),
                eq("submission-001"),
                any(AssessmentSubmissionRequest.class)
        )).thenReturn(result);

        mockMvc.perform(post(ENDPOINT + "/PHQ-9/submissions")
                        .header(USER_ID_HEADER, userId)
                        .header(USER_ROLE_HEADER, "ROLE_USER")
                        .header("Idempotency-Key", "submission-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assessmentVersion": 1,
                                  "answers": [
                                    {
                                      "questionId": "%s",
                                      "optionId": "%s"
                                    }
                                  ]
                                }
                                """.formatted(questionId, optionId)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Location", "/api/v1/assessment-results/" + resultId))
                .andExpect(jsonPath("$.resultId").value(resultId.toString()))
                .andExpect(jsonPath("$.assessmentCode").value("PHQ-9"))
                .andExpect(jsonPath("$.assessmentVersion").value(1))
                .andExpect(jsonPath("$.totalScore").value(8))
                .andExpect(jsonPath("$.riskLevel").value("MILD"))
                .andExpect(jsonPath("$.screeningNotice").value(
                        "Kết quả này chỉ có mục đích sàng lọc, không phải chẩn đoán."
                ))
                .andExpect(jsonPath("$.recommendations[0]").value("Theo dõi cảm xúc của bạn."))
                .andExpect(jsonPath("$.createdAt").value("2026-07-25T10:00:00Z"));

        verify(assessmentService).submitAssessment(
                userId,
                AssessmentCode.PHQ_9,
                "submission-001",
                new AssessmentSubmissionRequest(
                        1,
                        List.of(new AssessmentAnswerRequest(questionId, optionId))
                )
        );
    }

    @Test
    void submissionRequiresIdempotencyKey() throws Exception {
        mockMvc.perform(post(ENDPOINT + "/PHQ-9/submissions")
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSubmissionBody()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        verifyNoInteractions(assessmentService);
    }

    @Test
    void submissionRejectsInvalidNestedAnswerBeforeServiceCall() throws Exception {
        mockMvc.perform(post(ENDPOINT + "/PHQ-9/submissions")
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_USER")
                        .header("Idempotency-Key", "submission-invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "assessmentVersion": 1,
                                  "answers": [{"questionId": null, "optionId": null}]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isNotEmpty());

        verifyNoInteractions(assessmentService);
    }

    @Test
    void submissionReturnsVersionConflictFromService() throws Exception {
        when(assessmentService.submitAssessment(
                any(UUID.class),
                eq(AssessmentCode.PHQ_9),
                eq("submission-stale"),
                any(AssessmentSubmissionRequest.class)
        )).thenThrow(new ResourceConflictException(
                "ASSESSMENT_VERSION_CONFLICT",
                "The submitted assessment version is not currently published"
        ));

        mockMvc.perform(post(ENDPOINT + "/PHQ-9/submissions")
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_USER")
                        .header("Idempotency-Key", "submission-stale")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSubmissionBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ASSESSMENT_VERSION_CONFLICT"));
    }

    @Test
    void nonUserRoleCannotSubmitAssessment() throws Exception {
        mockMvc.perform(post(ENDPOINT + "/PHQ-9/submissions")
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_ADMIN")
                        .header("Idempotency-Key", "submission-admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validSubmissionBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        verifyNoInteractions(assessmentService);
    }

    private String validSubmissionBody() {
        return """
                {
                  "assessmentVersion": 1,
                  "answers": [
                    {
                      "questionId": "%s",
                      "optionId": "%s"
                    }
                  ]
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());
    }
}
