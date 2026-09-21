package com.mindcare.ai_service.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import com.mindcare.ai_service.dto.*;
import com.mindcare.ai_service.service.ChatExecutionService;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.*;
import tools.jackson.databind.ObjectMapper;

class AiChatWebSocketHandlerTest {
    @Test
    void socketPassesSameRetryIdToSharedExecutionService() throws Exception {
        UUID user = UUID.randomUUID(), requestId = UUID.randomUUID();
        var service = mock(ChatExecutionService.class);
        when(service.chat(eq(user), any())).thenReturn(new RagChatResponse("Hello", List.of()));
        var worker = Executors.newSingleThreadExecutor();
        try {
            var handler = new AiChatWebSocketHandler(new ObjectMapper(), service, worker);
            var session = mock(WebSocketSession.class);
            var headers = new HttpHeaders();
            headers.set("X-User-Id", user.toString());
            when(session.getHandshakeHeaders()).thenReturn(headers);
            when(session.isOpen()).thenReturn(true);
            handler.handleTextMessage(session, new TextMessage("""
                    {"type":"AI_QUESTION","requestId":"%s","question":"Hello","topK":5}
                    """.formatted(requestId)));
            var request = ArgumentCaptor.forClass(RagChatRequest.class);
            verify(service, timeout(2000)).chat(eq(user), request.capture());
            assertThat(request.getValue().requestId()).isEqualTo(requestId);
            verify(session, timeout(2000)).sendMessage(argThat(message -> message.getPayload().toString().contains("AI_RESPONSE")));
        } finally { worker.shutdownNow(); }
    }

    @Test
    void invalidIdNeverCallsTheProviderBoundary() throws Exception {
        var service = mock(ChatExecutionService.class);
        var worker = Executors.newSingleThreadExecutor();
        try {
            var handler = new AiChatWebSocketHandler(new ObjectMapper(), service, worker);
            var session = mock(WebSocketSession.class);
            when(session.isOpen()).thenReturn(true);
            handler.handleTextMessage(session, new TextMessage(
                    "{\"type\":\"AI_QUESTION\",\"requestId\":\"bad-id\",\"question\":\"Hello\"}"));
            verifyNoInteractions(service);
            verify(session).sendMessage(argThat(message -> message.getPayload().toString().contains("INVALID_AI_REQUEST")));
        } finally { worker.shutdownNow(); }
    }
}
