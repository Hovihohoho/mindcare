package com.mindcare.bookingservice.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mindcare.bookingservice.chat.dto.MessageResponse;
import com.mindcare.bookingservice.chat.dto.SendMessageRequest;
import com.mindcare.bookingservice.chat.entity.Conversation;
import com.mindcare.bookingservice.chat.entity.Message;
import com.mindcare.bookingservice.chat.mapper.ChatMapper;
import com.mindcare.bookingservice.chat.repository.ConversationRepository;
import com.mindcare.bookingservice.chat.repository.MessageRepository;
import com.mindcare.bookingservice.chat.websocket.ExpertChatSocketHub;
import com.mindcare.bookingservice.integration.auth.ExpertBookingProfile;
import com.mindcare.bookingservice.integration.auth.ExpertProfileGateway;
import com.mindcare.bookingservice.integration.outbox.service.OutboxService;
import com.mindcare.bookingservice.shared.exception.ResourceNotFoundException;
import com.mindcare.bookingservice.shared.web.CursorCodec;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {
    @Mock ConversationRepository conversationRepository;
    @Mock MessageRepository messageRepository;
    @Mock ExpertProfileGateway expertProfileGateway;
    @Mock ChatMapper chatMapper;
    @Mock OutboxService outboxService;
    @Mock ExpertChatSocketHub socketHub;

    private ChatServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ChatServiceImpl(
                conversationRepository, messageRepository, expertProfileGateway,
                chatMapper, new CursorCodec(), outboxService, socketHub,
                Clock.fixed(Instant.parse("2026-08-09T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void userCanCreateDirectConversationWithEligibleExpert() {
        UUID userId = UUID.randomUUID();
        UUID expertId = UUID.randomUUID();
        when(expertProfileGateway.getBookingProfile(expertId))
                .thenReturn(new ExpertBookingProfile(expertId, true, BigDecimal.ZERO, "VND"));
        when(conversationRepository.findByUserIdAndExpertUserIdAndDeletedAtIsNull(userId, expertId))
                .thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.getOrCreate(userId, expertId);

        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.expertUserId()).isEqualTo(expertId);
        assertThat(response.writable()).isTrue();
    }

    @Test
    void nonParticipantCannotReadConversation() {
        UUID userId = UUID.randomUUID();
        UUID expertId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        when(conversationRepository.findByIdAndDeletedAtIsNull(conversationId))
                .thenReturn(Optional.of(Conversation.create(userId, expertId,
                        java.time.OffsetDateTime.parse("2026-08-09T00:00:00Z"))));

        assertThatThrownBy(() -> service.getHistory(UUID.randomUUID(), conversationId, null, 20))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void participantCanSendMessage() {
        UUID userId = UUID.randomUUID();
        UUID expertId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        Conversation conversation = Conversation.create(userId, expertId,
                java.time.OffsetDateTime.parse("2026-08-09T00:00:00Z"));
        when(conversationRepository.findByIdAndDeletedAtIsNull(conversationId))
                .thenReturn(Optional.of(conversation));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message message = invocation.getArgument(0);
            ReflectionTestUtils.setField(message, "id", UUID.randomUUID());
            return message;
        });
        MessageResponse expected = new MessageResponse(
                UUID.randomUUID(), conversationId, userId,
                com.mindcare.bookingservice.chat.entity.MessageType.TEXT,
                "Xin chào", false, null,
                java.time.OffsetDateTime.parse("2026-08-09T00:00:00Z"));
        when(chatMapper.toResponse(any(Message.class))).thenReturn(expected);

        assertThat(service.send(userId, conversationId, new SendMessageRequest("Xin chào")))
                .isEqualTo(expected);
        verify(socketHub).publish(expertId, expected);
    }
}
