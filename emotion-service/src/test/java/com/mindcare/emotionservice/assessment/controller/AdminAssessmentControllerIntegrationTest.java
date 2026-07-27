package com.mindcare.emotionservice.assessment.controller;

import com.mindcare.emotionservice.assessment.dto.AdminAssessmentResponse;
import com.mindcare.emotionservice.assessment.dto.AdminAnswerOptionResponse;
import com.mindcare.emotionservice.assessment.dto.AdminQuestionResponse;
import com.mindcare.emotionservice.assessment.entity.AssessmentCode;
import com.mindcare.emotionservice.assessment.service.AssessmentService;
import com.mindcare.emotionservice.shared.dto.CursorPageResponse;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAssessmentControllerIntegrationTest {

    private static final String ENDPOINT = "/api/v1/admin/assessments";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssessmentService assessmentService;

    @Test
    void adminCanCreateAssessmentDraftWithQuestionsAndAnswerOptions() throws Exception {
        UUID assessmentId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        AdminAssessmentResponse response = new AdminAssessmentResponse(
                assessmentId,
                AssessmentCode.PHQ_9,
                1,
                "DRAFT",
                "PHQ-9",
                "Screening questionnaire",
                List.of(new AdminQuestionResponse(
                        questionId,
                        "Question 1",
                        0,
                        List.of(
                                new AdminAnswerOptionResponse(UUID.randomUUID(), "Never", 0),
                                new AdminAnswerOptionResponse(UUID.randomUUID(), "Several days", 1)
                        )
                )),
                OffsetDateTime.parse("2026-07-24T00:00:00Z"),
                OffsetDateTime.parse("2026-07-24T00:00:00Z")
        );
        when(assessmentService.createAssessment(any())).thenReturn(response);

        mockMvc.perform(post(ENDPOINT)
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "PHQ-9",
                                  "title": "PHQ-9",
                                  "description": "Screening questionnaire",
                                  "questions": [
                                    {
                                      "questionText": "Question 1",
                                      "orderIndex": 0
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/admin/assessments/" + assessmentId
                ))
                .andExpect(jsonPath("$.id").value(assessmentId.toString()))
                .andExpect(jsonPath("$.code").value("PHQ-9"))
                .andExpect(jsonPath("$.assessmentVersion").value(1))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.questions[0].id").value(questionId.toString()))
                .andExpect(jsonPath("$.questions[0].answerOptions[0].scoreValue").value(0));

        verify(assessmentService).createAssessment(any());
    }

    @Test
    void createAssessmentRejectsInvalidNestedCatalogBeforeServiceCall() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "WHO-5",
                                  "title": "",
                                  "questions": [
                                    {
                                      "questionText": "",
                                      "orderIndex": -1
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isNotEmpty());

        verifyNoInteractions(assessmentService);
    }

    @Test
    void createAssessmentRejectsUnsupportedCodeBeforeServiceCall() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "CUSTOM",
                                  "title": "Custom",
                                  "questions": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        verifyNoInteractions(assessmentService);
    }

    @Test
    void createAssessmentRejectsManuallySuppliedAnswerOptions() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "PHQ-9",
                                  "title": "PHQ-9",
                                  "questions": [
                                    {
                                      "questionText": "Question 1",
                                      "orderIndex": 0,
                                      "answerOptions": [
                                        {"optionText": "Manual option", "scoreValue": 99}
                                      ]
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field")
                        .value("questions[0].answerOptions"));

        verifyNoInteractions(assessmentService);
    }

    @Test
    void createAssessmentMapsDuplicateCodeToConflict() throws Exception {
        when(assessmentService.createAssessment(any())).thenThrow(new ResourceConflictException(
                "ASSESSMENT_CODE_CONFLICT",
                "assessment code already exists"
        ));

        mockMvc.perform(post(ENDPOINT)
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validDraftJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ASSESSMENT_CODE_CONFLICT"));
    }

    @Test
    void userRoleCannotCreateAssessmentDraft() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validDraftJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        verifyNoInteractions(assessmentService);
    }

    @Test
    void adminCanReplaceAssessmentDraftCatalog() throws Exception {
        UUID assessmentId = UUID.randomUUID();
        AdminAssessmentResponse response = new AdminAssessmentResponse(
                assessmentId,
                AssessmentCode.GAD_7,
                1,
                "DRAFT",
                "GAD-7 revised",
                "Revised description",
                List.of(new AdminQuestionResponse(
                        UUID.randomUUID(),
                        "Revised question",
                        0,
                        List.of(
                                new AdminAnswerOptionResponse(UUID.randomUUID(), "Never", 0),
                                new AdminAnswerOptionResponse(UUID.randomUUID(), "Every day", 3)
                        )
                )),
                OffsetDateTime.parse("2026-07-24T00:00:00Z"),
                OffsetDateTime.parse("2026-07-24T01:00:00Z")
        );
        when(assessmentService.updateAssessment(eq(assessmentId), any())).thenReturn(response);

        mockMvc.perform(put(ENDPOINT + "/{assessmentId}", assessmentId)
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "GAD-7",
                                  "title": "GAD-7 revised",
                                  "description": "Revised description",
                                  "questions": [
                                    {
                                      "questionText": "Revised question",
                                      "orderIndex": 0
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(assessmentId.toString()))
                .andExpect(jsonPath("$.code").value("GAD-7"))
                .andExpect(jsonPath("$.assessmentVersion").value(1))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.title").value("GAD-7 revised"))
                .andExpect(jsonPath("$.questions[0].questionText").value("Revised question"));

        verify(assessmentService).updateAssessment(eq(assessmentId), any());
    }

    @Test
    void updateAssessmentRejectsInvalidNestedCatalogBeforeServiceCall() throws Exception {
        mockMvc.perform(put(ENDPOINT + "/{assessmentId}", UUID.randomUUID())
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "GAD-7",
                                  "title": "",
                                  "questions": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(assessmentService);
    }

    @Test
    void updateAssessmentMapsInvalidLifecycleToConflict() throws Exception {
        UUID assessmentId = UUID.randomUUID();
        when(assessmentService.updateAssessment(eq(assessmentId), any()))
                .thenThrow(new ResourceConflictException(
                        "INVALID_STATE_TRANSITION",
                        "Only a draft assessment can be updated"
                ));

        mockMvc.perform(put(ENDPOINT + "/{assessmentId}", assessmentId)
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validDraftJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    void userRoleCannotUpdateAssessmentDraft() throws Exception {
        mockMvc.perform(put(ENDPOINT + "/{assessmentId}", UUID.randomUUID())
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validDraftJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        verifyNoInteractions(assessmentService);
    }

    @Test
    void adminCanCreateNextAssessmentVersionFromPublishedAssessment() throws Exception {
        UUID sourceId = UUID.randomUUID();
        UUID draftId = UUID.randomUUID();
        AdminAssessmentResponse response = new AdminAssessmentResponse(
                draftId,
                AssessmentCode.PHQ_9,
                2,
                "DRAFT",
                "PHQ-9",
                null,
                List.of(),
                OffsetDateTime.parse("2026-07-25T00:00:00Z"),
                OffsetDateTime.parse("2026-07-25T00:00:00Z")
        );
        when(assessmentService.createNextAssessmentVersion(sourceId)).thenReturn(response);

        mockMvc.perform(post(ENDPOINT + "/{assessmentId}:create-next-version", sourceId)
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_ADMIN"))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/admin/assessments/" + draftId
                ))
                .andExpect(jsonPath("$.id").value(draftId.toString()))
                .andExpect(jsonPath("$.assessmentVersion").value(2))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        verify(assessmentService).createNextAssessmentVersion(sourceId);
    }

    @Test
    void createNextAssessmentVersionMapsExistingDraftToConflict() throws Exception {
        UUID sourceId = UUID.randomUUID();
        when(assessmentService.createNextAssessmentVersion(sourceId))
                .thenThrow(new ResourceConflictException(
                        "ASSESSMENT_DRAFT_VERSION_EXISTS",
                        "A draft version already exists for this assessment code"
                ));

        mockMvc.perform(post(ENDPOINT + "/{assessmentId}:create-next-version", sourceId)
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_ADMIN"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ASSESSMENT_DRAFT_VERSION_EXISTS"));
    }

    @Test
    void userRoleCannotCreateNextAssessmentVersion() throws Exception {
        mockMvc.perform(post(ENDPOINT + "/{assessmentId}:create-next-version", UUID.randomUUID())
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_USER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        verifyNoInteractions(assessmentService);
    }

    @Test
    void adminCanPublishAssessmentDraft() throws Exception {
        UUID assessmentId = UUID.randomUUID();
        when(assessmentService.publishAssessment(assessmentId))
                .thenReturn(lifecycleResponse(assessmentId, "PUBLISHED"));

        mockMvc.perform(post(ENDPOINT + "/{assessmentId}:publish", assessmentId)
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(assessmentId.toString()))
                .andExpect(jsonPath("$.code").value("PHQ-9"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(assessmentService).publishAssessment(assessmentId);
    }

    @Test
    void publishAssessmentMapsIncompleteDraftToBadRequest() throws Exception {
        UUID assessmentId = UUID.randomUUID();
        when(assessmentService.publishAssessment(assessmentId))
                .thenThrow(new com.mindcare.emotionservice.shared.exception.InvalidRequestException(
                        "ASSESSMENT_INCOMPLETE",
                        "Assessment requires exactly 9 questions before publish"
                ));

        mockMvc.perform(post(ENDPOINT + "/{assessmentId}:publish", assessmentId)
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_ADMIN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ASSESSMENT_INCOMPLETE"));
    }

    @Test
    void adminCanArchivePublishedAssessment() throws Exception {
        UUID assessmentId = UUID.randomUUID();
        when(assessmentService.archiveAssessment(assessmentId))
                .thenReturn(lifecycleResponse(assessmentId, "ARCHIVED"));

        mockMvc.perform(post(ENDPOINT + "/{assessmentId}:archive", assessmentId)
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(assessmentId.toString()))
                .andExpect(jsonPath("$.status").value("ARCHIVED"));

        verify(assessmentService).archiveAssessment(assessmentId);
    }

    @Test
    void archiveAssessmentMapsInvalidLifecycleToConflict() throws Exception {
        UUID assessmentId = UUID.randomUUID();
        when(assessmentService.archiveAssessment(assessmentId))
                .thenThrow(new ResourceConflictException(
                        "INVALID_STATE_TRANSITION",
                        "Only a published assessment can be archived"
                ));

        mockMvc.perform(post(ENDPOINT + "/{assessmentId}:archive", assessmentId)
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_ADMIN"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    void userRoleCannotPublishOrArchiveAssessment() throws Exception {
        UUID assessmentId = UUID.randomUUID();

        mockMvc.perform(post(ENDPOINT + "/{assessmentId}:publish", assessmentId)
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_USER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mockMvc.perform(post(ENDPOINT + "/{assessmentId}:archive", assessmentId)
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_USER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        verifyNoInteractions(assessmentService);
    }

    @Test
    void adminCanGetAssessmentDetailInAnyLifecycleStatus() throws Exception {
        UUID assessmentId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID optionId = UUID.randomUUID();
        AdminAssessmentResponse response = new AdminAssessmentResponse(
                assessmentId,
                AssessmentCode.PHQ_9,
                2,
                "ARCHIVED",
                "PHQ-9 version 2",
                "Archived catalog",
                List.of(new AdminQuestionResponse(
                        questionId,
                        "Question 1",
                        0,
                        List.of(new AdminAnswerOptionResponse(optionId, "Không hề", 0))
                )),
                OffsetDateTime.parse("2026-07-25T00:00:00Z"),
                OffsetDateTime.parse("2026-07-25T01:00:00Z")
        );
        when(assessmentService.getAssessmentForAdmin(assessmentId)).thenReturn(response);

        mockMvc.perform(get(ENDPOINT + "/{assessmentId}", assessmentId)
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(assessmentId.toString()))
                .andExpect(jsonPath("$.code").value("PHQ-9"))
                .andExpect(jsonPath("$.assessmentVersion").value(2))
                .andExpect(jsonPath("$.status").value("ARCHIVED"))
                .andExpect(jsonPath("$.questions[0].id").value(questionId.toString()))
                .andExpect(jsonPath("$.questions[0].answerOptions[0].id").value(optionId.toString()))
                .andExpect(jsonPath("$.questions[0].answerOptions[0].scoreValue").value(0));

        verify(assessmentService).getAssessmentForAdmin(assessmentId);
    }

    @Test
    void getAssessmentDetailMapsMissingResourceToNotFound() throws Exception {
        UUID assessmentId = UUID.randomUUID();
        when(assessmentService.getAssessmentForAdmin(assessmentId))
                .thenThrow(new ResourceNotFoundException("Assessment"));

        mockMvc.perform(get(ENDPOINT + "/{assessmentId}", assessmentId)
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_ADMIN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void userRoleCannotGetAdminAssessmentDetail() throws Exception {
        mockMvc.perform(get(ENDPOINT + "/{assessmentId}", UUID.randomUUID())
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_USER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        verifyNoInteractions(assessmentService);
    }

    @Test
    void adminCanListAssessmentsWithStatusAndCursor() throws Exception {
        UUID assessmentId = UUID.randomUUID();
        AdminAssessmentResponse assessment = new AdminAssessmentResponse(
                assessmentId,
                AssessmentCode.PHQ_9,
                1,
                "DRAFT",
                "Patient Health Questionnaire",
                null,
                List.of(),
                OffsetDateTime.parse("2026-07-24T00:00:00Z"),
                OffsetDateTime.parse("2026-07-24T00:00:00Z")
        );
        when(assessmentService.getAssessmentsForAdmin("DRAFT", "opaque-cursor", 10))
                .thenReturn(new CursorPageResponse<>(List.of(assessment), "next-cursor", true));

        mockMvc.perform(get(ENDPOINT)
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_ADMIN")
                        .param("status", "DRAFT")
                        .param("cursor", "opaque-cursor")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items[0].id").value(assessmentId.toString()))
                .andExpect(jsonPath("$.items[0].code").value("PHQ-9"))
                .andExpect(jsonPath("$.items[0].status").value("DRAFT"))
                .andExpect(jsonPath("$.items[0].questions").isArray())
                .andExpect(jsonPath("$.nextCursor").value("next-cursor"))
                .andExpect(jsonPath("$.hasMore").value(true));

        verify(assessmentService).getAssessmentsForAdmin("DRAFT", "opaque-cursor", 10);
    }

    @Test
    void userRoleCannotAccessAdminAssessments() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_USER"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        verifyNoInteractions(assessmentService);
    }

    @Test
    void missingVerifiedRoleCannotAccessAdminAssessments() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .header(USER_ID_HEADER, UUID.randomUUID()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        verifyNoInteractions(assessmentService);
    }

    @Test
    void unsupportedVerifiedRoleIsRejectedAsUnauthorized() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ADMIN"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        verifyNoInteractions(assessmentService);
    }

    @Test
    void missingIdentityCannotUseRoleHeaderAlone() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .header(USER_ROLE_HEADER, "ROLE_ADMIN"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        verifyNoInteractions(assessmentService);
    }

    @Test
    void invalidLimitReturnsValidationErrorBeforeServiceCall() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_ADMIN")
                        .param("limit", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(assessmentService);
    }

    private String validDraftJson() {
        return """
                {
                  "code": "GAD-7",
                  "title": "GAD-7",
                  "questions": [
                    {
                      "questionText": "Question 1",
                      "orderIndex": 0
                    }
                  ]
                }
                """;
    }

    private AdminAssessmentResponse lifecycleResponse(UUID assessmentId, String status) {
        return new AdminAssessmentResponse(
                assessmentId,
                AssessmentCode.PHQ_9,
                1,
                status,
                "PHQ-9",
                null,
                List.of(),
                OffsetDateTime.parse("2026-07-25T00:00:00Z"),
                OffsetDateTime.parse("2026-07-25T00:00:00Z")
        );
    }
}
