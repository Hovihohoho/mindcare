package com.mindcare.bookingservice.consultation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mindcare.bookingservice.booking.service.BookingMaintenanceService;
import com.mindcare.bookingservice.consultation.dto.ConsultationNoteResponse;
import com.mindcare.bookingservice.consultation.dto.UserConsultationNoteResponse;
import com.mindcare.bookingservice.consultation.service.ConsultationNoteService;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConsultationNoteControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsultationNoteService consultationNoteService;

    @MockitoBean
    private BookingMaintenanceService bookingMaintenanceService;

    @Test
    void expertCanUpsertConsultationNote() throws Exception {
        UUID expertId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.parse("2026-07-25T00:00:00Z");
        ConsultationNoteResponse response = new ConsultationNoteResponse(
                noteId,
                bookingId,
                expertId,
                "Quan sát riêng",
                "Duy trì lịch ngủ đều đặn",
                "Theo dõi trong hai tuần",
                true,
                now,
                now);
        when(consultationNoteService.upsert(
                eq(expertId),
                eq(bookingId),
                any()))
                .thenReturn(response);

        mockMvc.perform(put(
                        "/api/v1/expert/bookings/{bookingId}/consultation-note",
                        bookingId)
                        .header("X-User-Id", expertId)
                        .header("X-User-Role", "ROLE_EXPERT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "observation": "Quan sát riêng",
                                  "recommendation": "Duy trì lịch ngủ đều đặn",
                                  "recoveryPlan": "Theo dõi trong hai tuần",
                                  "visibleToUser": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(noteId.toString()))
                .andExpect(jsonPath("$.bookingId").value(bookingId.toString()))
                .andExpect(jsonPath("$.visibleToUser").value(true));

        verify(consultationNoteService).upsert(
                eq(expertId),
                eq(bookingId),
                any());
    }

    @Test
    void expertCanReadFullConsultationNote() throws Exception {
        UUID expertId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        ConsultationNoteResponse response = expertResponse(expertId, bookingId);
        when(consultationNoteService.getForExpert(expertId, bookingId))
                .thenReturn(response);

        mockMvc.perform(get(
                        "/api/v1/expert/bookings/{bookingId}/consultation-note",
                        bookingId)
                        .header("X-User-Id", expertId)
                        .header("X-User-Role", "ROLE_EXPERT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId")
                        .value(bookingId.toString()))
                .andExpect(jsonPath("$.observation")
                        .value("Quan sát riêng"));

        verify(consultationNoteService)
                .getForExpert(expertId, bookingId);
    }

    @Test
    void userCanReadOnlyVisibleConsultationNoteFields() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();
        OffsetDateTime updatedAt =
                OffsetDateTime.parse("2026-07-25T00:00:00Z");
        when(consultationNoteService.getForUser(userId, bookingId))
                .thenReturn(new UserConsultationNoteResponse(
                        noteId,
                        bookingId,
                        "Duy trì lịch ngủ đều đặn",
                        "Theo dõi trong hai tuần",
                        updatedAt));

        mockMvc.perform(get(
                        "/api/v1/bookings/{bookingId}/consultation-note",
                        bookingId)
                        .header("X-User-Id", userId)
                        .header("X-User-Role", "ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(noteId.toString()))
                .andExpect(jsonPath("$.recommendation")
                        .value("Duy trì lịch ngủ đều đặn"))
                .andExpect(jsonPath("$.observation").doesNotExist())
                .andExpect(jsonPath("$.expertUserId").doesNotExist());

        verify(consultationNoteService).getForUser(userId, bookingId);
    }

    private ConsultationNoteResponse expertResponse(
            UUID expertId,
            UUID bookingId) {
        OffsetDateTime now = OffsetDateTime.parse("2026-07-25T00:00:00Z");
        return new ConsultationNoteResponse(
                UUID.randomUUID(),
                bookingId,
                expertId,
                "Quan sát riêng",
                "Duy trì lịch ngủ đều đặn",
                "Theo dõi trong hai tuần",
                true,
                now,
                now);
    }
}
