package com.mindcare.bookingservice.chat.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindcare.bookingservice.chat.dto.MessageResponse;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
@RequiredArgsConstructor
public class ExpertChatSocketHub {

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<UUID, Set<WebSocketSession>> sessions =
            new ConcurrentHashMap<>();

    public void register(UUID userId, WebSocketSession session) {
        sessions.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet())
                .add(session);
    }

    public void unregister(UUID userId, WebSocketSession session) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null) {
            return;
        }
        userSessions.remove(session);
        if (userSessions.isEmpty()) {
            sessions.remove(userId, userSessions);
        }
    }

    public void publish(UUID userId, MessageResponse message) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null || userSessions.isEmpty()) {
            return;
        }
        try {
            TextMessage payload = new TextMessage(objectMapper.writeValueAsString(
                    new ExpertChatEvent("EXPERT_MESSAGE_CREATED", message)));
            userSessions.removeIf(session -> !send(session, payload));
        } catch (IOException ignored) {
            // The REST write already succeeded; realtime delivery can be retried by refetching.
        }
    }

    private boolean send(WebSocketSession session, TextMessage payload) {
        if (!session.isOpen()) {
            return false;
        }
        try {
            synchronized (session) {
                session.sendMessage(payload);
            }
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private record ExpertChatEvent(String type, MessageResponse message) {}
}
