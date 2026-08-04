package com.mindcare.auth_service.notification;

import tools.jackson.databind.ObjectMapper;
import com.mindcare.auth_service.dto.NotificationDtos;
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
public class NotificationSocketHub {

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<UUID, Set<WebSocketSession>> sessions =
            new ConcurrentHashMap<>();

    public void register(UUID userId, WebSocketSession session) {
        sessions.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet())
                .add(session);
    }

    public void unregister(UUID userId, WebSocketSession session) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null) return;
        userSessions.remove(session);
        if (userSessions.isEmpty()) sessions.remove(userId, userSessions);
    }

    public void publish(UUID userId, NotificationDtos.Item notification) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null || userSessions.isEmpty()) return;
        TextMessage payload = new TextMessage(objectMapper.writeValueAsString(
                new NotificationEvent("NOTIFICATION_CREATED", notification)));
        userSessions.removeIf(session -> !send(session, payload));
    }

    private boolean send(WebSocketSession session, TextMessage payload) {
        if (!session.isOpen()) return false;
        try {
            synchronized (session) {
                session.sendMessage(payload);
            }
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    private record NotificationEvent(
            String type,
            NotificationDtos.Item notification) {}
}
