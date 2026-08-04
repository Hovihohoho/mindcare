package com.mindcare.bookingservice.chat.service;

import com.mindcare.bookingservice.chat.dto.ConversationResponse;
import com.mindcare.bookingservice.chat.dto.ConversationHistoryResponse;
import com.mindcare.bookingservice.chat.dto.MessageResponse;
import com.mindcare.bookingservice.chat.dto.SendMessageRequest;
import com.mindcare.bookingservice.shared.dto.CursorPageResponse;
import java.util.UUID;

public interface ChatService {

    ConversationResponse getOrCreate(UUID actorId, UUID bookingId);

    CursorPageResponse<ConversationHistoryResponse> getConversationHistory(
            UUID actorId,
            String cursor,
            int limit);

    CursorPageResponse<MessageResponse> getHistory(
            UUID actorId,
            UUID conversationId,
            String cursor,
            int limit);

    MessageResponse send(
            UUID actorId,
            UUID conversationId,
            SendMessageRequest request);

    int markRead(UUID actorId, UUID conversationId);
}
