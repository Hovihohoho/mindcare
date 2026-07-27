package com.mindcare.emotionservice.journal.controller;

import com.mindcare.emotionservice.journal.dto.CreateEmotionJournalRequest;
import com.mindcare.emotionservice.journal.dto.EmotionJournalResponse;
import com.mindcare.emotionservice.journal.entity.EmotionType;
import com.mindcare.emotionservice.journal.exception.JournalDeletionWindowExpiredException;
import com.mindcare.emotionservice.journal.service.EmotionJournalService;
import com.mindcare.emotionservice.shared.dto.CursorPageResponse;
import com.mindcare.emotionservice.shared.exception.ResourceNotFoundException;
import com.mindcare.emotionservice.shared.exception.InvalidRequestException;
import com.mindcare.emotionservice.shared.web.RequestContext;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmotionJournalControllerIntegrationTest {

    private static final String ENDPOINT = "/api/v1/emotion-journals";
    private static final String USER_ID_HEADER = "X-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmotionJournalService emotionJournalService;

    @Test
    void createJournalReturnsCreatedResourceAndLocationForAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID journalId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-07-24T03:30:00Z");
        EmotionJournalResponse response = new EmotionJournalResponse(
                journalId,
                EmotionType.SAD,
                "Hôm nay mình thấy quá tải.",
                createdAt,
                createdAt
        );
        when(emotionJournalService.createJournal(eq(userId), any(CreateEmotionJournalRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post(ENDPOINT)
                        .header(USER_ID_HEADER, userId)
                        .header(RequestContext.CORRELATION_ID_HEADER, "journal-create-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "emotionType": "SAD",
                                  "content": "Hôm nay mình thấy quá tải."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(
                        "Location",
                        "/api/v1/emotion-journals/" + journalId
                ))
                .andExpect(header().string(
                        RequestContext.CORRELATION_ID_HEADER,
                        "journal-create-test"
                ))
                .andExpect(jsonPath("$.id").value(journalId.toString()))
                .andExpect(jsonPath("$.emotionType").value("SAD"))
                .andExpect(jsonPath("$.content").value("Hôm nay mình thấy quá tải."))
                .andExpect(jsonPath("$.createdAt").value("2026-07-24T03:30:00Z"))
                .andExpect(jsonPath("$.updatedAt").value("2026-07-24T03:30:00Z"));

        verify(emotionJournalService).createJournal(
                eq(userId),
                eq(new CreateEmotionJournalRequest(
                        EmotionType.SAD,
                        "Hôm nay mình thấy quá tải."
                ))
        );
    }

    @Test
    void createJournalWithoutAuthenticatedUserReturnsUnauthorized() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "emotionType": "HAPPY",
                                  "content": "Một ngày tốt."
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        verifyNoInteractions(emotionJournalService);
    }

    @Test
    void createJournalWithoutEmotionTypeReturnsFieldValidationError() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Thiếu loại cảm xúc."
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'emotionType')].code")
                        .value("NOT_NULL"));

        verifyNoInteractions(emotionJournalService);
    }

    @Test
    void createJournalWithUnknownEmotionTypeReturnsMalformedRequest() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "emotionType": "ANGRY",
                                  "content": "Giá trị enum chưa được hỗ trợ."
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        verifyNoInteractions(emotionJournalService);
    }

    @Test
    void createJournalWithMoreThanFiveThousandCodePointsReturnsValidationError() throws Exception {
        String content = "a".repeat(5_001);
        String requestBody = """
                {
                  "emotionType": "STRESSED",
                  "content": "%s"
                }
                """.formatted(content);

        mockMvc.perform(post(ENDPOINT)
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'content')].code")
                        .value("MAX_CODE_POINTS"));

        verifyNoInteractions(emotionJournalService);
    }

    @Test
    void getJournalReturnsOwnedJournalForAuthenticatedUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID journalId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-07-24T03:30:00Z");
        EmotionJournalResponse response = new EmotionJournalResponse(
                journalId,
                EmotionType.HAPPY,
                "Hôm nay là một ngày tốt.",
                createdAt,
                createdAt
        );
        when(emotionJournalService.getJournal(userId, journalId)).thenReturn(response);

        mockMvc.perform(get(ENDPOINT + "/{journalId}", journalId)
                        .header(USER_ID_HEADER, userId)
                        .header(RequestContext.CORRELATION_ID_HEADER, "journal-detail-test"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(header().string(
                        RequestContext.CORRELATION_ID_HEADER,
                        "journal-detail-test"
                ))
                .andExpect(jsonPath("$.id").value(journalId.toString()))
                .andExpect(jsonPath("$.emotionType").value("HAPPY"))
                .andExpect(jsonPath("$.content").value("Hôm nay là một ngày tốt."))
                .andExpect(jsonPath("$.createdAt").value("2026-07-24T03:30:00Z"))
                .andExpect(jsonPath("$.updatedAt").value("2026-07-24T03:30:00Z"));

        verify(emotionJournalService).getJournal(userId, journalId);
    }

    @Test
    void getJournalWithoutAuthenticatedUserReturnsUnauthorized() throws Exception {
        mockMvc.perform(get(ENDPOINT + "/{journalId}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        verifyNoInteractions(emotionJournalService);
    }

    @Test
    void getJournalWithMalformedIdReturnsMalformedRequest() throws Exception {
        mockMvc.perform(get(ENDPOINT + "/not-a-uuid")
                        .header(USER_ID_HEADER, UUID.randomUUID()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        verifyNoInteractions(emotionJournalService);
    }

    @Test
    void getJournalUnavailableToUserReturnsNonDisclosingNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID journalId = UUID.randomUUID();
        when(emotionJournalService.getJournal(userId, journalId))
                .thenThrow(new ResourceNotFoundException("Emotion journal"));

        mockMvc.perform(get(ENDPOINT + "/{journalId}", journalId)
                        .header(USER_ID_HEADER, userId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("Emotion journal was not found"));

        verify(emotionJournalService).getJournal(userId, journalId);
    }

    @Test
    void getJournalHistoryReturnsCursorPageForRequestedMonth() throws Exception {
        UUID userId = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.parse("2026-10-01T00:00:00+07:00");
        OffsetDateTime to = OffsetDateTime.parse("2026-11-01T00:00:00+07:00");
        EmotionJournalResponse newest = journalResponse(
                UUID.randomUUID(),
                EmotionType.HAPPY,
                "Ngày đầu tháng.",
                "2026-10-31T14:30:00Z"
        );
        EmotionJournalResponse older = journalResponse(
                UUID.randomUUID(),
                EmotionType.NEUTRAL,
                null,
                "2026-10-30T02:00:00Z"
        );
        CursorPageResponse<EmotionJournalResponse> page =
                new CursorPageResponse<>(List.of(newest, older), "next-page-cursor", true);
        when(emotionJournalService.getJournalHistory(
                userId,
                from,
                to,
                "current-cursor",
                2
        )).thenReturn(page);

        mockMvc.perform(get(ENDPOINT)
                        .header(USER_ID_HEADER, userId)
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .param("cursor", "current-cursor")
                        .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].id").value(newest.id().toString()))
                .andExpect(jsonPath("$.items[1].id").value(older.id().toString()))
                .andExpect(jsonPath("$.nextCursor").value("next-page-cursor"))
                .andExpect(jsonPath("$.hasMore").value(true));

        verify(emotionJournalService).getJournalHistory(
                userId,
                from,
                to,
                "current-cursor",
                2
        );
    }

    @Test
    void getJournalHistoryUsesDefaultLimitAndNullCursor() throws Exception {
        UUID userId = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.parse("2026-07-20T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-07-27T00:00:00Z");
        when(emotionJournalService.getJournalHistory(userId, from, to, null, 20))
                .thenReturn(new CursorPageResponse<>(List.of(), null, false));

        mockMvc.perform(get(ENDPOINT)
                        .header(USER_ID_HEADER, userId)
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.nextCursor").doesNotExist())
                .andExpect(jsonPath("$.hasMore").value(false));

        verify(emotionJournalService).getJournalHistory(userId, from, to, null, 20);
    }

    @Test
    void getJournalHistoryRejectsLimitAboveMaximumAtHttpBoundary() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .param("from", "2026-10-01T00:00:00+07:00")
                        .param("to", "2026-11-01T00:00:00+07:00")
                        .param("limit", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field == 'limit')].code")
                        .value("MAX"));

        verifyNoInteractions(emotionJournalService);
    }

    @Test
    void getJournalHistoryRejectsMalformedTimestamp() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .param("from", "01-10-2026")
                        .param("to", "2026-11-01T00:00:00+07:00"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        verifyNoInteractions(emotionJournalService);
    }

    @Test
    void getJournalHistoryRejectsInvalidTimeRangeFromService() throws Exception {
        UUID userId = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.parse("2026-11-01T00:00:00+07:00");
        OffsetDateTime to = OffsetDateTime.parse("2026-10-01T00:00:00+07:00");
        when(emotionJournalService.getJournalHistory(userId, from, to, null, 20))
                .thenThrow(new InvalidRequestException(
                        "INVALID_TIME_RANGE",
                        "from must be before to"
                ));

        mockMvc.perform(get(ENDPOINT)
                        .header(USER_ID_HEADER, userId)
                        .param("from", from.toString())
                        .param("to", to.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_TIME_RANGE"));

        verify(emotionJournalService).getJournalHistory(userId, from, to, null, 20);
    }

    @Test
    void deleteJournalReturnsNoContentForOwnedJournalWithinDeletionWindow() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID journalId = UUID.randomUUID();

        mockMvc.perform(delete(ENDPOINT + "/{journalId}", journalId)
                        .header(USER_ID_HEADER, userId)
                        .header(RequestContext.CORRELATION_ID_HEADER, "journal-delete-test"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(
                        RequestContext.CORRELATION_ID_HEADER,
                        "journal-delete-test"
                ))
                .andExpect(content().string(""));

        verify(emotionJournalService).deleteJournal(userId, journalId);
    }

    @Test
    void deleteJournalAfterDeletionWindowReturnsBusinessRuleViolation() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID journalId = UUID.randomUUID();
        doThrow(new JournalDeletionWindowExpiredException())
                .when(emotionJournalService)
                .deleteJournal(userId, journalId);

        mockMvc.perform(delete(ENDPOINT + "/{journalId}", journalId)
                        .header(USER_ID_HEADER, userId))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("JOURNAL_DELETION_WINDOW_EXPIRED"));

        verify(emotionJournalService).deleteJournal(userId, journalId);
    }

    @Test
    void deleteJournalUnavailableToUserReturnsNonDisclosingNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID journalId = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Emotion journal"))
                .when(emotionJournalService)
                .deleteJournal(userId, journalId);

        mockMvc.perform(delete(ENDPOINT + "/{journalId}", journalId)
                        .header(USER_ID_HEADER, userId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        verify(emotionJournalService).deleteJournal(userId, journalId);
    }

    @Test
    void deleteJournalWithoutAuthenticatedUserReturnsUnauthorized() throws Exception {
        mockMvc.perform(delete(ENDPOINT + "/{journalId}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        verifyNoInteractions(emotionJournalService);
    }

    @Test
    void deleteJournalWithMalformedIdReturnsMalformedRequest() throws Exception {
        mockMvc.perform(delete(ENDPOINT + "/not-a-uuid")
                        .header(USER_ID_HEADER, UUID.randomUUID()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        verifyNoInteractions(emotionJournalService);
    }

    private EmotionJournalResponse journalResponse(
            UUID id,
            EmotionType emotionType,
            String content,
            String timestamp
    ) {
        OffsetDateTime occurredAt = OffsetDateTime.parse(timestamp);
        return new EmotionJournalResponse(
                id,
                emotionType,
                content,
                occurredAt,
                occurredAt
        );
    }
}
