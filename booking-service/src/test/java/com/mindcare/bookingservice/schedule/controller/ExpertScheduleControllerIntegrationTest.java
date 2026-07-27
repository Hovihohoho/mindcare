package com.mindcare.bookingservice.schedule.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mindcare.bookingservice.schedule.dto.ScheduleResponse;
import com.mindcare.bookingservice.schedule.entity.ScheduleStatus;
import com.mindcare.bookingservice.booking.service.BookingMaintenanceService;
import com.mindcare.bookingservice.schedule.service.ScheduleLifecycleService;
import com.mindcare.bookingservice.schedule.service.ScheduleService;
import java.time.OffsetDateTime;
import java.util.List;
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
class ExpertScheduleControllerIntegrationTest {

    private static final String ENDPOINT = "/api/v1/expert/schedules";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean(
            name = "scheduleServiceImpl",
            extraInterfaces = ScheduleLifecycleService.class)
    private ScheduleService scheduleService;

    @MockitoBean
    private BookingMaintenanceService bookingMaintenanceService;

    @Test
    void expertCanCreateSchedule() throws Exception {
        UUID expertId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        OffsetDateTime startAt = OffsetDateTime.parse("2030-08-01T08:00:00Z");
        OffsetDateTime endAt = startAt.plusHours(1);
        when(scheduleService.create(eq(expertId), any()))
                .thenReturn(new ScheduleResponse(
                        scheduleId,
                        expertId,
                        startAt,
                        endAt,
                        ScheduleStatus.AVAILABLE,
                        null,
                        OffsetDateTime.parse("2026-07-25T00:00:00Z"),
                        OffsetDateTime.parse("2026-07-25T00:00:00Z")));

        mockMvc.perform(post(ENDPOINT)
                        .header("X-User-Id", expertId)
                        .header("X-User-Role", "ROLE_EXPERT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startAt": "2030-08-01T08:00:00Z",
                                  "endAt": "2030-08-01T09:00:00Z"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        ENDPOINT + "/" + scheduleId))
                .andExpect(jsonPath("$.id").value(scheduleId.toString()))
                .andExpect(jsonPath("$.expertUserId").value(expertId.toString()))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));

        verify(scheduleService).create(eq(expertId), any());
    }

    @Test
    void expertCanListOwnSchedules() throws Exception {
        UUID expertId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.parse("2030-08-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2030-08-08T00:00:00Z");
        when(scheduleService.listOwn(expertId, from, to, 20))
                .thenReturn(List.of(new ScheduleResponse(
                        scheduleId,
                        expertId,
                        from.plusHours(8),
                        from.plusHours(9),
                        ScheduleStatus.AVAILABLE,
                        null,
                        OffsetDateTime.parse("2026-07-25T00:00:00Z"),
                        OffsetDateTime.parse("2026-07-25T00:00:00Z"))));

        mockMvc.perform(get(ENDPOINT)
                        .header("X-User-Id", expertId)
                        .header("X-User-Role", "ROLE_EXPERT")
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(scheduleId.toString()))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));

        verify(scheduleService).listOwn(expertId, from, to, 20);
    }

    @Test
    void expertCanUpdateOwnedAvailableSchedule() throws Exception {
        UUID expertId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        OffsetDateTime startAt = OffsetDateTime.parse("2030-08-02T08:00:00Z");
        OffsetDateTime endAt = startAt.plusHours(1);
        when(scheduleService.update(eq(expertId), eq(scheduleId), any()))
                .thenReturn(new ScheduleResponse(
                        scheduleId,
                        expertId,
                        startAt,
                        endAt,
                        ScheduleStatus.AVAILABLE,
                        null,
                        OffsetDateTime.parse("2026-07-25T00:00:00Z"),
                        OffsetDateTime.parse("2026-07-25T01:00:00Z")));

        mockMvc.perform(put(ENDPOINT + "/{scheduleId}", scheduleId)
                        .header("X-User-Id", expertId)
                        .header("X-User-Role", "ROLE_EXPERT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startAt": "2030-08-02T08:00:00Z",
                                  "endAt": "2030-08-02T09:00:00Z"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(scheduleId.toString()))
                .andExpect(jsonPath("$.startAt").value("2030-08-02T08:00:00Z"));

        verify(scheduleService).update(eq(expertId), eq(scheduleId), any());
    }

    @Test
    void expertCanCancelOwnedAvailableSchedule() throws Exception {
        UUID expertId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();

        mockMvc.perform(delete(ENDPOINT + "/{scheduleId}", scheduleId)
                        .header("X-User-Id", expertId)
                        .header("X-User-Role", "ROLE_EXPERT"))
                .andExpect(status().isNoContent());

        verify(scheduleService).cancel(expertId, scheduleId);
    }

    @Test
    void anyoneCanListAvailableSchedulesForExpert() throws Exception {
        UUID expertId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        OffsetDateTime from = OffsetDateTime.parse("2030-08-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2030-08-08T00:00:00Z");
        when(scheduleService.listAvailable(expertId, from, to, 20))
                .thenReturn(List.of(new ScheduleResponse(
                        scheduleId,
                        expertId,
                        from.plusHours(8),
                        from.plusHours(9),
                        ScheduleStatus.AVAILABLE,
                        null,
                        OffsetDateTime.parse("2026-07-25T00:00:00Z"),
                        OffsetDateTime.parse("2026-07-25T00:00:00Z"))));

        mockMvc.perform(get(
                        "/api/v1/experts/{expertUserId}/schedules",
                        expertId)
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(scheduleId.toString()))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));

        verify(scheduleService).listAvailable(expertId, from, to, 20);
    }

    @Test
    void userCannotCreateExpertSchedule() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .header("X-User-Id", UUID.randomUUID())
                        .header("X-User-Role", "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startAt": "2030-08-01T08:00:00Z",
                                  "endAt": "2030-08-01T09:00:00Z"
                                }
                                """))
                .andExpect(status().isForbidden());

        verifyNoInteractions(scheduleService);
    }

    @Test
    void invalidScheduleRequestIsRejectedBeforeService() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .header("X-User-Id", UUID.randomUUID())
                        .header("X-User-Role", "ROLE_EXPERT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startAt": null,
                                  "endAt": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verifyNoInteractions(scheduleService);
    }
}
