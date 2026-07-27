package com.mindcare.emotionservice.journal.controller;

import com.mindcare.emotionservice.journal.dto.EmotionTrendPointResponse;
import com.mindcare.emotionservice.journal.service.EmotionJournalService;
import com.mindcare.emotionservice.shared.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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
class EmotionTrendControllerIntegrationTest {

    private static final String ENDPOINT = "/api/v1/emotion-trends";
    private static final String USER_ID_HEADER = "X-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmotionJournalService emotionJournalService;

    @Test
    void getEmotionTrendsReturnsVersionedDailySeries() throws Exception {
        UUID userId = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.parse("2026-07-20T00:00:00+07:00");
        OffsetDateTime to = OffsetDateTime.parse("2026-07-27T00:00:00+07:00");
        ZoneId timezone = ZoneId.of("Asia/Ho_Chi_Minh");
        when(emotionJournalService.getEmotionTrends(
                userId,
                from,
                to,
                "DAY",
                timezone
        )).thenReturn(List.of(
                new EmotionTrendPointResponse(
                        from,
                        from.plusDays(1),
                        new BigDecimal("1.00"),
                        2,
                        "emotion-v1"
                ),
                new EmotionTrendPointResponse(
                        from.plusDays(1),
                        from.plusDays(2),
                        null,
                        0,
                        "emotion-v1"
                )
        ));

        mockMvc.perform(get(ENDPOINT)
                        .header(USER_ID_HEADER, userId)
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .param("bucket", "DAY")
                        .param("timezone", "Asia/Ho_Chi_Minh"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].averageScore").value(1.00))
                .andExpect(jsonPath("$[0].count").value(2))
                .andExpect(jsonPath("$[0].mappingVersion").value("emotion-v1"))
                .andExpect(jsonPath("$[1].averageScore").doesNotExist())
                .andExpect(jsonPath("$[1].count").value(0));

        verify(emotionJournalService).getEmotionTrends(
                userId,
                from,
                to,
                "DAY",
                timezone
        );
    }

    @Test
    void getEmotionTrendsWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .param("from", "2026-07-20T00:00:00Z")
                        .param("to", "2026-07-27T00:00:00Z")
                        .param("bucket", "DAY")
                        .param("timezone", "UTC"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));

        verifyNoInteractions(emotionJournalService);
    }

    @Test
    void getEmotionTrendsRejectsMalformedTimestamp() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .param("from", "20-07-2026")
                        .param("to", "2026-07-27T00:00:00Z")
                        .param("bucket", "DAY")
                        .param("timezone", "UTC"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));

        verifyNoInteractions(emotionJournalService);
    }

    @Test
    void getEmotionTrendsRejectsInvalidTimezone() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .header(USER_ID_HEADER, UUID.randomUUID())
                        .param("from", "2026-07-20T00:00:00Z")
                        .param("to", "2026-07-27T00:00:00Z")
                        .param("bucket", "DAY")
                        .param("timezone", "Invalid/Timezone"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TIMEZONE"));

        verifyNoInteractions(emotionJournalService);
    }

    @Test
    void getEmotionTrendsReturnsBucketValidationFromService() throws Exception {
        UUID userId = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.parse("2026-07-20T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-07-27T00:00:00Z");
        ZoneId timezone = ZoneId.of("UTC");
        when(emotionJournalService.getEmotionTrends(
                userId,
                from,
                to,
                "YEAR",
                timezone
        )).thenThrow(new InvalidRequestException(
                "INVALID_BUCKET",
                "bucket must be DAY, WEEK or MONTH"
        ));

        mockMvc.perform(get(ENDPOINT)
                        .header(USER_ID_HEADER, userId)
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .param("bucket", "YEAR")
                        .param("timezone", "UTC"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_BUCKET"));

        verify(emotionJournalService).getEmotionTrends(
                userId,
                from,
                to,
                "YEAR",
                timezone
        );
    }
}
