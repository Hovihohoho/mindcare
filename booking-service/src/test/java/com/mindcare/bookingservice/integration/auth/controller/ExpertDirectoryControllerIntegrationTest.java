package com.mindcare.bookingservice.integration.auth.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mindcare.bookingservice.booking.service.BookingMaintenanceService;
import com.mindcare.bookingservice.integration.auth.dto.ExpertSummaryResponse;
import com.mindcare.bookingservice.integration.auth.service.ExpertDirectoryService;
import com.mindcare.bookingservice.shared.dto.CursorPageResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExpertDirectoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExpertDirectoryService expertDirectoryService;

    @MockitoBean
    private BookingMaintenanceService bookingMaintenanceService;

    @Test
    void userCanListBookableExpertsWithCursorPagination() throws Exception {
        UUID expertId = UUID.randomUUID();
        ExpertSummaryResponse expert = new ExpertSummaryResponse(
                expertId,
                "Chuyên gia An",
                "Tham vấn tâm lý",
                List.of("STRESS"),
                8,
                new BigDecimal("300000.00"),
                "VND",
                new BigDecimal("4.80"),
                124);
        when(expertDirectoryService.listBookable(
                "an",
                "STRESS",
                "cursor-1",
                10))
                .thenReturn(new CursorPageResponse<>(
                        List.of(expert),
                        "cursor-2",
                        true));

        mockMvc.perform(get("/api/v1/experts")
                        .header("X-User-Id", UUID.randomUUID())
                        .header("X-User-Role", "ROLE_USER")
                        .param("keyword", "an")
                        .param("specialty", "STRESS")
                        .param("cursor", "cursor-1")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].expertUserId")
                        .value(expertId.toString()))
                .andExpect(jsonPath("$.items[0].consultationFee")
                        .value(300000.00))
                .andExpect(jsonPath("$.items[0].currency").value("VND"))
                .andExpect(jsonPath("$.nextCursor").value("cursor-2"))
                .andExpect(jsonPath("$.hasMore").value(true));

        verify(expertDirectoryService).listBookable(
                "an",
                "STRESS",
                "cursor-1",
                10);
    }

    @Test
    void expertRoleCannotUseUserExpertCatalog() throws Exception {
        mockMvc.perform(get("/api/v1/experts")
                        .header("X-User-Id", UUID.randomUUID())
                        .header("X-User-Role", "ROLE_EXPERT"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        verifyNoInteractions(expertDirectoryService);
    }
}
