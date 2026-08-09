package com.mindcare.bookingservice.chat.service;

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
import com.mindcare.bookingservice.integration.auth.ExpertBookingProfile;
import com.mindcare.bookingservice.integration.auth.ExpertProfileGateway;
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
    private final ExpertProfileGateway expertProfileGateway;
    private final ChatMapper chatMapper;
    private final CursorCodec cursorCodec;
    private final OutboxService outboxService;
    private final ExpertChatSocketHub expertChatSocketHub;
    private final Clock clock;

    @Override
    @Transactional
    public ConversationResponse getOrCreate(UUID userId, UUID expertUserId) {
        if (userId.equals(expertUserId)) {
            throw new BusinessException(
                    "INVALID_CONVERSATION_PARTICIPANTS",
                    HttpStatus.BAD_REQUEST,
                    "User and expert must be different accounts");
        }
        ExpertBookingProfile expert = expertProfileGateway.getBookingProfile(expertUserId);
        if (!expert.eligible()) {
            throw new ResourceNotFoundException();
        }
        Conversation conversation = conversationRepository
                .findByUserIdAndExpertUserIdAndDeletedAtIsNull(userId, expertUserId)
                .orElse(null);
        if (conversation != null) {
            return toConversationResponse(conversation);
        }
        conversation = conversationRepository.save(
                Conversation.create(userId, expertUserId, OffsetDateTime.now(clock)));
        return toConversationResponse(conversation);
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationResponse getConversation(UUID actorId, UUID conversationId) {
        Conversation conversation = getConversationEntity(conversationId);
        requireParticipant(actorId, conversation);
        return toConversationResponse(conversation);
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
                page.stream().map(conversation -> new ConversationHistoryResponse(
                        conversation.getId(),
                        conversation.getUserId(),
                        conversation.getExpertUserId(),
                        conversation.getOpenedAt(),
                        conversation.getClosedAt(),
                        conversation.getClosedAt() == null)).toList(),
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
        Conversation conversation = getConversationEntity(conversationId);
        requireParticipant(actorId, conversation);
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
        Conversation conversation = getConversationEntity(conversationId);
        requireParticipant(actorId, conversation);
        requireWritable(conversation);
        if (request.content() == null || request.content().isBlank()) {
            throw new BusinessException(
                    "MESSAGE_CONTENT_REQUIRED",
                    HttpStatus.BAD_REQUEST,
                    "Text message content is required");
        }
        Message message = messageRepository.save(
                Message.text(conversationId, actorId, request.content()));
        UUID recipientId = conversation.getUserId().equals(actorId)
                ? conversation.getExpertUserId()
                : conversation.getUserId();
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
        Conversation conversation = getConversationEntity(conversationId);
        requireParticipant(actorId, conversation);
        return messageRepository.markConversationRead(
                conversationId,
                actorId,
                OffsetDateTime.now(clock));
    }

    private Conversation getConversationEntity(UUID conversationId) {
        return conversationRepository.findByIdAndDeletedAtIsNull(conversationId)
                .orElseThrow(ResourceNotFoundException::new);
    }

    private void requireParticipant(UUID actorId, Conversation conversation) {
        if (!conversation.isParticipant(actorId)) {
            throw new ResourceNotFoundException();
        }
    }

    private void requireWritable(Conversation conversation) {
        if (conversation.getClosedAt() != null) {
            throw new BusinessException(
                    "CONVERSATION_CLOSED",
                    HttpStatus.CONFLICT,
                    "Conversation is read-only because it has been closed");
        }
    }

    private ConversationResponse toConversationResponse(Conversation conversation) {
        return new ConversationResponse(
                conversation.getId(),
                conversation.getUserId(),
                conversation.getExpertUserId(),
                conversation.getOpenedAt(),
                conversation.getClosedAt(),
                conversation.getClosedAt() == null);
    }

    private int normalizeLimit(int limit) {
        return Math.max(1, Math.min(limit, 100));
    }
}
