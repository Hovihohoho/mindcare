package com.mindcare.ai_service.websocket;

import tools.jackson.databind.ObjectMapper;
import com.mindcare.ai_service.dto.RagChatRequest;
import com.mindcare.ai_service.dto.RagChatResponse;
import com.mindcare.ai_service.service.RagChatService;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@RequiredArgsConstructor
public class AiChatWebSocketHandler extends TextWebSocketHandler {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final ObjectMapper objectMapper;
    private final RagChatService ragChatService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String rawUserId = session.getHandshakeHeaders().getFirst(USER_ID_HEADER);
        try {
            UUID.fromString(rawUserId);
            send(session, Map.of("type", "AI_SOCKET_READY"));
        } catch (RuntimeException invalidIdentity) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Missing verified identity"));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        AiRequest request;
        try {
            request = objectMapper.readValue(message.getPayload(), AiRequest.class);
            if (!"AI_QUESTION".equals(request.type())
                    || request.requestId() == null
                    || request.requestId().isBlank()
                    || request.question() == null
                    || request.question().isBlank()
                    || request.question().length() > 4000) {
                throw new IllegalArgumentException("Invalid AI request");
            }
        } catch (RuntimeException invalidRequest) {
            send(session, Map.of(
                    "type", "AI_ERROR",
                    "code", "INVALID_AI_REQUEST",
                    "message", "Câu hỏi không hợp lệ."));
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                RagChatResponse response = ragChatService.chat(
                        new RagChatRequest(request.question(), request.topK(), request.history()));
                send(session, Map.of(
                        "type", "AI_RESPONSE",
                        "requestId", request.requestId(),
                        "data", response));
            } catch (RuntimeException error) {
                send(session, Map.of(
                        "type", "AI_ERROR",
                        "requestId", request.requestId(),
                        "code", "AI_SERVICE_ERROR",
                        "message", "Không thể xử lý câu hỏi lúc này."));
            }
        });
    }

    private void send(WebSocketSession session, Object payload) {
        if (!session.isOpen()) {
            return;
        }
        try {
            TextMessage message =
                    new TextMessage(objectMapper.writeValueAsString(payload));
            synchronized (session) {
                session.sendMessage(message);
            }
        } catch (IOException ignored) {
            // The client reconnects and can retry the request.
        }
    }

    private record AiRequest(
            String type,
            String requestId,
            String question,
            Integer topK,
            java.util.List<RagChatRequest.ConversationMessage> history) {}
}
