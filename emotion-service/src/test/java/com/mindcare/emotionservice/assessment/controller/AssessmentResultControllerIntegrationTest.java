package com.mindcare.emotionservice.assessment.controller;

import com.mindcare.emotionservice.assessment.dto.AssessmentResultResponse;
import com.mindcare.emotionservice.assessment.entity.AssessmentCode;
import com.mindcare.emotionservice.assessment.service.AssessmentService;
import com.mindcare.emotionservice.shared.dto.CursorPageResponse;
import com.mindcare.emotionservice.shared.exception.InvalidRequestException;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AssessmentResultControllerIntegrationTest {

    private static final String ENDPOINT = "/api/v1/assessment-results";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssessmentService assessmentService;

    @Test
    void userCanGetAssessmentHistoryWithKeysetPagination() throws Exception {
        UUID userId = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.parse("2026-07-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-08-01T00:00:00Z");
        AssessmentResultResponse newest = result(
                UUID.randomUUID(),
                AssessmentCode.PHQ_9,
                12,
                "MODERATE",
                "2026-07-25T10:00:00Z"
        );
        AssessmentResultResponse older = result(
                UUID.randomUUID(),
                AssessmentCode.GAD_7,
                4,
                "NORMAL",
                "2026-07-20T09:00:00Z"
        );
        when(assessmentService.getAssessmentHistory(
                userId,
                from,
                to,
                "current-cursor",
                2
        )).thenReturn(new CursorPageResponse<>(
                List.of(newest, older),
                "next-cursor",
                true
        ));

        mockMvc.perform(get(ENDPOINT)
                        .header(USER_ID_HEADER, userId)
                        .header(USER_ROLE_HEADER, "ROLE_USER")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .param("cursor", "current-cursor")
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].resultId").value(newest.resultId().toString()))
                .andExpect(jsonPath("$.items[0].assessmentCode").value("PHQ-9"))
                .andExpect(jsonPath("$.items[0].totalScore").value(12))
                .andExpect(jsonPath("$.items[1].resultId").value(older.resultId().toString()))
                .andExpect(jsonPath("$.nextCursor").value("next-cursor"))
                .andExpect(jsonPath("$.hasMore").value(true));

        verify(assessmentService).getAssessmentHistory(
                userId,
                from,
                to,
                "current-cursor",
                2
        );
    }

    @Test
    void assessmentHistoryUsesDefaultLimitAndNullCursor() throws Exception {
        UUID userId = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.parse("2026-07-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-08-01T00:00:00Z");
        when(assessmentService.getAssessmentHistory(userId, from, to, null, 20))
                .thenReturn(new CursorPageResponse<>(List.of(), null, false));

        mockMvc.perform(get(ENDPOINT)
                        .header(USER_ID_HEADER, userId)
                        .header(USER_ROLE_HEADER, "ROLE_USER")
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.nextCursor").doesNotExist())
                .andExpect(jsonPath("$.hasMore").value(false));

        verify(assessmentService).getAssessmentHistory(userId, from, to, null, 20);
    }

    @Test
    void assessmentHistoryRejectsLimitAboveMaximumAtHttpBoundary() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_USER")
                        .param("from", "2026-07-01T00:00:00Z")
                        .param("to", "2026-08-01T00:00:00Z")
                        .param("limit", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verifyNoInteractions(assessmentService);
    }

    @Test
    void assessmentHistoryMapsInvalidRangeFromService() throws Exception {
        UUID userId = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.parse("2026-08-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-07-01T00:00:00Z");
        when(assessmentService.getAssessmentHistory(userId, from, to, null, 20))
                .thenThrow(new InvalidRequestException(
                        "INVALID_TIME_RANGE",
                        "from must be before to"
                ));

        mockMvc.perform(get(ENDPOINT)
                        .header(USER_ID_HEADER, userId)
                        .header(USER_ROLE_HEADER, "ROLE_USER")
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TIME_RANGE"));
    }

    @Test
    void userCanGetOwnedAssessmentResultDetail() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();
        AssessmentResultResponse response = result(
                resultId,
                AssessmentCode.PHQ_9,
                8,
                "MILD",
                "2026-07-25T10:00:00Z"
        );
        when(assessmentService.getAssessmentResult(userId, resultId)).thenReturn(response);

        mockMvc.perform(get(ENDPOINT + "/{resultId}", resultId)
                        .header(USER_ID_HEADER, userId)
                        .header(USER_ROLE_HEADER, "ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.resultId").value(resultId.toString()))
                .andExpect(jsonPath("$.assessmentCode").value("PHQ-9"))
                .andExpect(jsonPath("$.assessmentVersion").value(1))
                .andExpect(jsonPath("$.totalScore").value(8))
                .andExpect(jsonPath("$.riskLevel").value("MILD"))
                .andExpect(jsonPath("$.screeningNotice").value("Screening notice"))
                .andExpect(jsonPath("$.recommendations[0]").value("Safe recommendation"))
                .andExpect(jsonPath("$.createdAt").value("2026-07-25T10:00:00Z"));

        verify(assessmentService).getAssessmentResult(userId, resultId);
    }

    @Test
    void unavailableOrForeignAssessmentResultReturnsNonDisclosingNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID resultId = UUID.randomUUID();
        when(assessmentService.getAssessmentResult(userId, resultId))
                .thenThrow(new ResourceNotFoundException("Assessment result"));

        mockMvc.perform(get(ENDPOINT + "/{resultId}", resultId)
                        .header(USER_ID_HEADER, userId)
                        .header(USER_ROLE_HEADER, "ROLE_USER"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("Assessment result was not found"));
    }

    @Test
    void malformedResultIdReturnsMalformedRequestBeforeServiceCall() throws Exception {
        mockMvc.perform(get(ENDPOINT + "/not-a-uuid")
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_USER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        verifyNoInteractions(assessmentService);
    }

    @Test
    void missingIdentityCannotReadAssessmentResults() throws Exception {
        mockMvc.perform(get(ENDPOINT + "/{resultId}", UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_USER"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        verifyNoInteractions(assessmentService);
    }

    @Test
    void nonUserRoleCannotReadAssessmentResults() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .header(USER_ROLE_HEADER, "ROLE_ADMIN")
                        .param("from", "2026-07-01T00:00:00Z")
                        .param("to", "2026-08-01T00:00:00Z"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        verifyNoInteractions(assessmentService);
    }

    private AssessmentResultResponse result(
            UUID resultId,
            AssessmentCode code,
            int totalScore,
            String riskLevel,
            String createdAt
    ) {
        return new AssessmentResultResponse(
                resultId,
                code,
                1,
                totalScore,
                riskLevel,
                "Screening notice",
                List.of("Safe recommendation"),
                OffsetDateTime.parse(createdAt)
        );
    }
}
