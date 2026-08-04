package com.mindcare.bookingservice.chat.service;

import com.mindcare.bookingservice.booking.entity.BookingStatus;
import com.mindcare.bookingservice.booking.service.BookingAccessService;
import com.mindcare.bookingservice.booking.service.BookingAccessSnapshot;
import com.mindcare.bookingservice.chat.dto.ConversationResponse;
import com.mindcare.bookingservice.chat.dto.ConversationHistoryResponse;
import com.mindcare.bookingservice.chat.dto.MessageResponse;
import com.mindcare.bookingservice.chat.dto.SendMessageRequest;
import com.mindcare.bookingservice.chat.entity.Conversation;
import com.mindcare.bookingservice.chat.entity.Message;
import com.mindcare.bookingservice.chat.mapper.ChatMapper;
import com.mindcare.bookingservice.chat.repository.ConversationRepository;
import com.mindcare.bookingservice.chat.repository.MessageRepository;
import com.mindcare.bookingservice.chat.websocket.ExpertChatSocketHub;
import com.mindcare.bookingservice.integration.outbox.service.OutboxService;
import com.mindcare.bookingservice.shared.config.BookingPolicyProperties;
import com.mindcare.bookingservice.shared.dto.CursorPageResponse;
import com.mindcare.bookingservice.shared.dto.PageCursor;
import com.mindcare.bookingservice.shared.exception.BusinessException;
import com.mindcare.bookingservice.shared.exception.ResourceNotFoundException;
import com.mindcare.bookingservice.shared.web.CursorCodec;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final BookingAccessService bookingAccessService;
    private final ChatMapper chatMapper;
    private final CursorCodec cursorCodec;
    private final OutboxService outboxService;
    private final ExpertChatSocketHub expertChatSocketHub;
    private final BookingPolicyProperties policy;
    private final Clock clock;

    @Override
    @Transactional
    public ConversationResponse getOrCreate(UUID actorId, UUID bookingId) {
        BookingAccessSnapshot access = bookingAccessService.getRequiredForUpdate(bookingId);
        if (!access.isParticipant(actorId)) {
            throw new ResourceNotFoundException();
        }
        Conversation conversation = conversationRepository
                .findByBookingIdAndDeletedAtIsNull(bookingId)
                .orElse(null);
        if (conversation != null) {
            return toConversationResponse(conversation, access);
        }
        requireWritableWindow(access);
        conversation = conversationRepository.save(
                Conversation.create(bookingId, OffsetDateTime.now(clock)));
        return toConversationResponse(conversation, access);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<ConversationHistoryResponse> getConversationHistory(
            UUID actorId,
            String cursor,
            int limit) {
        PageCursor decoded = cursorCodec.decode(cursor);
        int pageSize = normalizeLimit(limit);
        PageRequest pageRequest = PageRequest.of(0, pageSize + 1);
        List<Conversation> source = decoded.createdAt() == null
                ? conversationRepository.findHistoryForParticipant(actorId, pageRequest)
                : conversationRepository.findHistoryForParticipantAfter(
                        actorId,
                        decoded.createdAt(),
                        decoded.id(),
                        pageRequest);
        boolean hasMore = source.size() > pageSize;
        List<Conversation> page = hasMore ? source.subList(0, pageSize) : source;
        String nextCursor = null;
        if (hasMore && !page.isEmpty()) {
            Conversation last = page.get(page.size() - 1);
            nextCursor = cursorCodec.encode(last.getOpenedAt(), last.getId());
        }
        return new CursorPageResponse<>(
                page.stream().map(conversation -> {
                    BookingAccessSnapshot access =
                            bookingAccessService.getRequired(conversation.getBookingId());
                    return new ConversationHistoryResponse(
                            conversation.getId(),
                            conversation.getBookingId(),
                            access.userId(),
                            access.expertUserId(),
                            access.status(),
                            access.startAt(),
                            access.endAt(),
                            conversation.getOpenedAt());
                }).toList(),
                nextCursor,
                hasMore);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<MessageResponse> getHistory(
            UUID actorId,
            UUID conversationId,
            String cursor,
            int limit) {
        Conversation conversation = getConversation(conversationId);
        requireParticipant(actorId, conversation.getBookingId());
        PageCursor decoded = cursorCodec.decode(cursor);
        int pageSize = normalizeLimit(limit);
        PageRequest pageRequest = PageRequest.of(0, pageSize + 1);
        List<Message> source = decoded.createdAt() == null
                ? messageRepository.findHistory(conversationId, pageRequest)
                : messageRepository.findHistoryAfter(
                        conversationId,
                        decoded.createdAt(),
                        decoded.id(),
                        pageRequest);
        boolean hasMore = source.size() > pageSize;
        List<Message> page = hasMore ? source.subList(0, pageSize) : source;
        String nextCursor = null;
        if (hasMore && !page.isEmpty()) {
            Message last = page.get(page.size() - 1);
            nextCursor = cursorCodec.encode(last.getCreatedAt(), last.getId());
        }
        return new CursorPageResponse<>(
                page.stream().map(chatMapper::toResponse).toList(),
                nextCursor,
                hasMore);
    }

    @Override
    @Transactional
    public MessageResponse send(
            UUID actorId,
            UUID conversationId,
            SendMessageRequest request) {
        Conversation conversation = getConversation(conversationId);
        BookingAccessSnapshot access =
                requireParticipant(actorId, conversation.getBookingId());
        requireWritableWindow(access);
        if (request.content() == null || request.content().isBlank()) {
            throw new BusinessException(
                    "MESSAGE_CONTENT_REQUIRED",
                    HttpStatus.BAD_REQUEST,
                    "Text message content is required");
        }
        Message message = messageRepository.save(
                Message.text(conversationId, actorId, request.content()));
        UUID recipientId = access.userId().equals(actorId)
                ? access.expertUserId()
                : access.userId();
        outboxService.append(
                "MESSAGE",
                message.getId(),
                "expert.message.created",
                Map.of(
                        "messageId", message.getId(),
                        "conversationId", conversationId,
                        "senderId", actorId,
                        "recipientId", recipientId));
        MessageResponse response = chatMapper.toResponse(message);
        expertChatSocketHub.publish(actorId, response);
        expertChatSocketHub.publish(recipientId, response);
        return response;
    }

    @Override
    @Transactional
    public int markRead(UUID actorId, UUID conversationId) {
        Conversation conversation = getConversation(conversationId);
        requireParticipant(actorId, conversation.getBookingId());
        return messageRepository.markConversationRead(
                conversationId,
                actorId,
                OffsetDateTime.now(clock));
    }

    private Conversation getConversation(UUID conversationId) {
        return conversationRepository.findByIdAndDeletedAtIsNull(conversationId)
                .orElseThrow(ResourceNotFoundException::new);
    }

    private BookingAccessSnapshot requireParticipant(UUID actorId, UUID bookingId) {
        BookingAccessSnapshot access = bookingAccessService.getRequired(bookingId);
        if (!access.isParticipant(actorId)) {
            throw new ResourceNotFoundException();
        }
        return access;
    }

    private void requireWritableWindow(BookingAccessSnapshot access) {
        if (!isWritableWindow(access)) {
            throw new BusinessException(
                    "CHAT_WINDOW_CLOSED",
                    HttpStatus.CONFLICT,
                    "Chat is read-only outside the consultation window");
        }
    }

    private boolean isWritableWindow(BookingAccessSnapshot access) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        boolean validStatus = access.status() == BookingStatus.CONFIRMED
                || access.status() == BookingStatus.CANCELLATION_PENDING
                || access.status() == BookingStatus.COMPLETED;
        boolean inWindow = !now.isBefore(access.startAt().minus(policy.chatOpenBefore()))
                && !now.isAfter(access.endAt().plus(policy.chatCloseAfter()));
        return validStatus && inWindow;
    }

    private ConversationResponse toConversationResponse(
            Conversation conversation,
            BookingAccessSnapshot access) {
        return new ConversationResponse(
                conversation.getId(),
                conversation.getBookingId(),
                conversation.getOpenedAt(),
                conversation.getClosedAt(),
                isWritableWindow(access));
    }

    private int normalizeLimit(int limit) {
        return Math.max(1, Math.min(limit, 100));
    }
}
