package com.mindcare.bookingservice.chat.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mindcare.bookingservice.booking.service.BookingMaintenanceService;
import com.mindcare.bookingservice.chat.dto.ConversationHistoryResponse;
import com.mindcare.bookingservice.chat.dto.ConversationResponse;
import com.mindcare.bookingservice.chat.service.ChatService;
import com.mindcare.bookingservice.shared.dto.CursorPageResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatControllerIntegrationTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean ChatService chatService;
    @MockitoBean BookingMaintenanceService bookingMaintenanceService;

    @Test
    void userCanStartDirectConversation() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID expertId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        when(chatService.getOrCreate(userId, expertId)).thenReturn(new ConversationResponse(
                conversationId, userId, expertId,
                OffsetDateTime.parse("2026-08-09T00:00:00Z"), null, true));

        mockMvc.perform(post("/api/v1/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expertUserId\":\"" + expertId + "\"}")
                        .header("X-User-Id", userId)
                        .header("X-User-Role", "ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(conversationId.toString()))
                .andExpect(jsonPath("$.expertUserId").value(expertId.toString()));
        verify(chatService).getOrCreate(userId, expertId);
    }

    @Test
    void participantCanListDirectConversations() throws Exception {
        UUID actorId = UUID.randomUUID();
        UUID expertId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        OffsetDateTime openedAt = OffsetDateTime.parse("2026-08-09T00:00:00Z");
        when(chatService.getConversationHistory(actorId, null, 20)).thenReturn(
                new CursorPageResponse<>(List.of(new ConversationHistoryResponse(
                        conversationId, actorId, expertId, openedAt, null, true)), null, false));

        mockMvc.perform(get("/api/v1/conversations")
                        .header("X-User-Id", actorId)
                        .header("X-User-Role", "ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].expertUserId").value(expertId.toString()))
                .andExpect(jsonPath("$.items[0].writable").value(true));
    }
}
