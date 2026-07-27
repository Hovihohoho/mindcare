package com.mindcare.bookingservice.chat.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mindcare.bookingservice.booking.service.BookingMaintenanceService;
import com.mindcare.bookingservice.chat.dto.ConversationResponse;
import com.mindcare.bookingservice.chat.dto.MessageResponse;
import com.mindcare.bookingservice.chat.entity.MessageType;
import com.mindcare.bookingservice.chat.service.ChatService;
import com.mindcare.bookingservice.shared.dto.CursorPageResponse;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private BookingMaintenanceService bookingMaintenanceService;

    @Test
    void participantCanGetOrCreateBookingConversation() throws Exception {
        UUID actorId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        when(chatService.getOrCreate(actorId, bookingId))
                .thenReturn(new ConversationResponse(
                        conversationId,
                        bookingId,
                        OffsetDateTime.parse("2026-07-25T00:00:00Z"),
                        null));

        mockMvc.perform(post(
                        "/api/v1/bookings/{bookingId}/conversation",
                        bookingId)
                        .header("X-User-Id", actorId)
                        .header("X-User-Role", "ROLE_USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(conversationId.toString()))
                .andExpect(jsonPath("$.bookingId")
                        .value(bookingId.toString()));

        verify(chatService).getOrCreate(actorId, bookingId);
    }

    @Test
    void participantCanGetMessageHistory() throws Exception {
        UUID actorId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        MessageResponse message = new MessageResponse(
                messageId,
                conversationId,
                UUID.randomUUID(),
                MessageType.TEXT,
                "Xin chào",
                false,
                null,
                OffsetDateTime.parse("2026-07-25T00:00:00Z"));
        when(chatService.getHistory(
                actorId,
                conversationId,
                "cursor-1",
                20))
                .thenReturn(new CursorPageResponse<>(
                        List.of(message),
                        null,
                        false));

        mockMvc.perform(get(
                        "/api/v1/conversations/{conversationId}/messages",
                        conversationId)
                        .header("X-User-Id", actorId)
                        .header("X-User-Role", "ROLE_USER")
                        .param("cursor", "cursor-1")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id")
                        .value(messageId.toString()))
                .andExpect(jsonPath("$.items[0].messageType")
                        .value("TEXT"))
                .andExpect(jsonPath("$.hasMore").value(false));

        verify(chatService).getHistory(
                actorId,
                conversationId,
                "cursor-1",
                20);
    }

    @Test
    void participantCanSendTextMessage() throws Exception {
        UUID actorId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        MessageResponse response = new MessageResponse(
                messageId,
                conversationId,
                actorId,
                MessageType.TEXT,
                "Xin chào chuyên gia",
                false,
                null,
                OffsetDateTime.parse("2026-07-25T00:00:00Z"));
        when(chatService.send(
                eq(actorId),
                eq(conversationId),
                any()))
                .thenReturn(response);

        mockMvc.perform(post(
                        "/api/v1/conversations/{conversationId}/messages",
                        conversationId)
                        .header("X-User-Id", actorId)
                        .header("X-User-Role", "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "Xin chào chuyên gia"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(messageId.toString()))
                .andExpect(jsonPath("$.content")
                        .value("Xin chào chuyên gia"));

        verify(chatService).send(
                eq(actorId),
                eq(conversationId),
                any());
    }

    @Test
    void participantCanMarkConversationRead() throws Exception {
        UUID actorId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        when(chatService.markRead(actorId, conversationId))
                .thenReturn(3);

        mockMvc.perform(post(
                        "/api/v1/conversations/{conversationId}:read",
                        conversationId)
                        .header("X-User-Id", actorId)
                        .header("X-User-Role", "ROLE_EXPERT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedMessages").value(3));

        verify(chatService).markRead(actorId, conversationId);
    }
}
