package com.mindcare.auth_service.notification;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@RequiredArgsConstructor
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ID_ATTRIBUTE = "mindcare.userId";

    private final NotificationSocketHub socketHub;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String rawUserId = session.getHandshakeHeaders().getFirst(USER_ID_HEADER);
        try {
            UUID userId = UUID.fromString(rawUserId);
            session.getAttributes().put(USER_ID_ATTRIBUTE, userId);
            socketHub.register(userId, session);
            session.sendMessage(new TextMessage(
                    "{\"type\":\"NOTIFICATION_SOCKET_READY\"}"));
        } catch (RuntimeException invalidIdentity) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason(
                    "Missing verified identity"));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message)
            throws Exception {
        if ("PING".equalsIgnoreCase(message.getPayload())) {
            session.sendMessage(new TextMessage("{\"type\":\"PONG\"}"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object userId = session.getAttributes().get(USER_ID_ATTRIBUTE);
        if (userId instanceof UUID value) {
            socketHub.unregister(value, session);
        }
    }
}
