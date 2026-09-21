package com.mindcare.ai_service.websocket;

import tools.jackson.databind.ObjectMapper;
import com.mindcare.ai_service.dto.RagChatRequest;
import com.mindcare.ai_service.dto.RagChatResponse;
import com.mindcare.ai_service.service.ChatExecutionService;
import com.mindcare.ai_service.exception.ChatRequestException;
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
    private final ChatExecutionService conversationService;
    private final java.util.concurrent.ExecutorService aiChatExecutor;

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
            UUID.fromString(request.requestId());
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

        try {
            CompletableFuture.runAsync(() -> {
                try {
                    UUID userId = UUID.fromString(session.getHandshakeHeaders().getFirst(USER_ID_HEADER));
                    RagChatResponse response = conversationService.chat(
                            userId,
                            new RagChatRequest(
                                    request.question(), request.topK(), request.history(), request.conversationId(),
                                    UUID.fromString(request.requestId())));
                    send(session, Map.of(
                            "type", "AI_RESPONSE",
                            "requestId", request.requestId(),
                            "data", response));
                } catch (RuntimeException error) {
                    send(session, Map.of(
                            "type", "AI_ERROR",
                            "requestId", request.requestId(),
                            "code", error instanceof ChatRequestException specific
                                    ? specific.status().name() : "AI_SERVICE_ERROR",
                            "message", error instanceof ChatRequestException
                                    ? error.getMessage() : "Không thể xử lý câu hỏi lúc này."));
                }
            }, aiChatExecutor);
        } catch (java.util.concurrent.RejectedExecutionException busy) {
            send(session, Map.of("type", "AI_ERROR", "requestId", request.requestId(),
                    "code", "TOO_MANY_REQUESTS", "message", "Trợ lý đang bận. Vui lòng thử lại sau."));
        }
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
            java.util.List<RagChatRequest.ConversationMessage> history,
            UUID conversationId) {}
}
