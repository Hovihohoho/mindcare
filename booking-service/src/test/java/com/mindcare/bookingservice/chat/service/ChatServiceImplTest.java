package com.mindcare.bookingservice.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mindcare.bookingservice.booking.entity.BookingPaymentStatus;
import com.mindcare.bookingservice.booking.entity.BookingStatus;
import com.mindcare.bookingservice.booking.service.BookingAccessService;
import com.mindcare.bookingservice.booking.service.BookingAccessSnapshot;
import com.mindcare.bookingservice.chat.entity.Conversation;
import com.mindcare.bookingservice.chat.mapper.ChatMapper;
import com.mindcare.bookingservice.chat.repository.ConversationRepository;
import com.mindcare.bookingservice.chat.repository.MessageRepository;
import com.mindcare.bookingservice.chat.websocket.ExpertChatSocketHub;
import com.mindcare.bookingservice.integration.outbox.service.OutboxService;
import com.mindcare.bookingservice.shared.config.BookingPolicyProperties;
import com.mindcare.bookingservice.shared.web.CursorCodec;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    private static final OffsetDateTime NOW =
            OffsetDateTime.parse("2026-07-30T15:00:00Z");

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private BookingAccessService bookingAccessService;
    @Mock
    private ChatMapper chatMapper;
    @Mock
    private OutboxService outboxService;
    @Mock
    private ExpertChatSocketHub expertChatSocketHub;

    private ChatServiceImpl service;
    private UUID actorId;
    private UUID conversationId;

    @BeforeEach
    void setUp() {
        service = new ChatServiceImpl(
                conversationRepository,
                messageRepository,
                bookingAccessService,
                chatMapper,
                new CursorCodec(),
                outboxService,
                expertChatSocketHub,
                new BookingPolicyProperties(
                        Duration.ofMinutes(15),
                        Duration.ofHours(24),
                        Duration.ofHours(2),
                        Duration.ofHours(1),
                        Duration.ofHours(24),
                        Duration.ofHours(24),
                        Duration.ofMinutes(15),
                        Duration.ofHours(24)),
                Clock.fixed(NOW.toInstant(), ZoneOffset.UTC));
        actorId = UUID.randomUUID();
        conversationId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        UUID expertId = UUID.randomUUID();
        Conversation conversation = Conversation.create(bookingId, NOW);
        lenient().when(conversationRepository.findByIdAndDeletedAtIsNull(conversationId))
                .thenReturn(Optional.of(conversation));
        lenient().when(bookingAccessService.getRequired(bookingId))
                .thenReturn(new BookingAccessSnapshot(
                        bookingId,
                        actorId,
                        expertId,
                        UUID.randomUUID(),
                        BookingStatus.CONFIRMED,
                        BookingPaymentStatus.PAID,
                        NOW.minusHours(1),
                        NOW.plusHours(1)));
    }

    @Test
    void historyWithoutCursorUsesQueryWithoutNullableCursorParameters() {
        PageRequest pageRequest = PageRequest.of(0, 101);
        when(messageRepository.findHistory(conversationId, pageRequest))
                .thenReturn(List.of());

        var response = service.getHistory(actorId, conversationId, null, 100);

        assertThat(response.items()).isEmpty();
        verify(messageRepository).findHistory(conversationId, pageRequest);
        verify(messageRepository, never()).findHistoryAfter(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void historyWithCursorUsesKeysetQuery() {
        UUID cursorId = UUID.randomUUID();
        OffsetDateTime cursorCreatedAt = NOW.minusMinutes(5);
        String cursor = new CursorCodec().encode(cursorCreatedAt, cursorId);
        PageRequest pageRequest = PageRequest.of(0, 21);
        when(messageRepository.findHistoryAfter(
                        conversationId,
                        cursorCreatedAt,
                        cursorId,
                        pageRequest))
                .thenReturn(List.of());

        var response = service.getHistory(actorId, conversationId, cursor, 20);

        assertThat(response.items()).isEmpty();
        verify(messageRepository).findHistoryAfter(
                conversationId,
                cursorCreatedAt,
                cursorId,
                pageRequest);
        verify(messageRepository, never()).findHistory(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void existingConversationRemainsReadableAfterWriteWindowCloses() {
        UUID bookingId = UUID.randomUUID();
        UUID expertId = UUID.randomUUID();
        Conversation conversation = Conversation.create(
                bookingId,
                NOW.minusDays(4));
        when(bookingAccessService.getRequiredForUpdate(bookingId))
                .thenReturn(new BookingAccessSnapshot(
                        bookingId,
                        actorId,
                        expertId,
                        UUID.randomUUID(),
                        BookingStatus.COMPLETED,
                        BookingPaymentStatus.PAID,
                        NOW.minusDays(3),
                        NOW.minusDays(3).plusHours(1)));
        when(conversationRepository.findByBookingIdAndDeletedAtIsNull(bookingId))
                .thenReturn(Optional.of(conversation));

        var response = service.getOrCreate(actorId, bookingId);

        assertThat(response.writable()).isFalse();
        assertThat(response.bookingId()).isEqualTo(bookingId);
        verify(conversationRepository, never()).save(
                org.mockito.ArgumentMatchers.any());
    }
}
