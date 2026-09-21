package com.mindcare.ai_service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.mindcare.ai_service.dto.*;
import com.mindcare.ai_service.exception.ChatRequestException;
import com.mindcare.ai_service.service.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = "ai.chat.requests-per-minute=2")
@Import(PgVectorTestConfiguration.class)
class ChatExecutionIntegrationTest {
    @Autowired ChatExecutionService execution;
    @Autowired AiConversationService conversations;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean RagChatService rag;

    @BeforeEach
    void answer() {
        when(rag.chat(any())).thenReturn(new RagChatResponse("Hello", List.of()));
    }

    @Test
    void retryReplaysAcrossRequestsAndDeletionCascadesCachedContent() {
        UUID user = UUID.randomUUID();
        var request = request(UUID.randomUUID(), "Hello");
        var first = execution.chat(user, request);
        var replay = execution.chat(user, request);
        assertThat(replay).isEqualTo(first);
        verify(rag, times(1)).chat(any());
        assertThat(conversations.get(user, first.conversationId()).messages()).hasSize(2);
        conversations.delete(user, first.conversationId());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ai_schema.ai_chat_requests WHERE user_id=?",
                Integer.class, user)).isZero();
        assertThat(jdbc.queryForObject("SELECT request_count FROM ai_schema.ai_chat_rate_limits WHERE user_id=?",
                Integer.class, user)).isEqualTo(1);
    }

    @Test
    void sameKeyWithDifferentPayloadIsRejectedAndUsersAreIsolated() {
        UUID key = UUID.randomUUID();
        UUID firstUser = UUID.randomUUID();
        var first = execution.chat(firstUser, request(key, "One"));
        assertThatThrownBy(() -> execution.chat(firstUser, request(key, "Two")))
                .isInstanceOfSatisfying(ChatRequestException.class,
                        error -> assertThat(error.status().value()).isEqualTo(409));
        var second = execution.chat(UUID.randomUUID(), request(key, "One"));
        assertThat(first.conversationId()).isNotEqualTo(second.conversationId());
    }

    @Test
    void quotaRejectsNewRequestsButAllowsReplayAndCannotBeResetByDeletingHistory() {
        UUID user = UUID.randomUUID();
        var first = request(UUID.randomUUID(), "One");
        execution.chat(user, first);
        execution.chat(user, request(UUID.randomUUID(), "Two"));
        execution.chat(user, first);
        conversations.deleteAll(user);
        assertThatThrownBy(() -> execution.chat(user, request(UUID.randomUUID(), "Three")))
                .isInstanceOfSatisfying(ChatRequestException.class,
                        error -> assertThat(error.status().value()).isEqualTo(429));
        verify(rag, times(2)).chat(any());
    }

    @Test
    void concurrentDeliveryDoesNotCallProviderTwice() throws Exception {
        UUID user = UUID.randomUUID();
        var request = request(UUID.randomUUID(), "One");
        var entered = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        when(rag.chat(any())).thenAnswer(call -> {
            entered.countDown();
            if (!release.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("test timed out");
            return new RagChatResponse("Hello", List.of());
        });
        var worker = Executors.newSingleThreadExecutor();
        try {
            var original = worker.submit(() -> execution.chat(user, request));
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> execution.chat(user, request)).isInstanceOf(ChatRequestException.class);
            release.countDown();
            var response = original.get(10, TimeUnit.SECONDS);
            assertThat(execution.chat(user, request)).isEqualTo(response);
            verify(rag, times(1)).chat(any());
        } finally {
            release.countDown();
            worker.shutdownNow();
        }
    }

    @Test
    void exportReadsPastFiftyRowsWithoutIncludingAnotherUser() {
        UUID user = UUID.randomUUID();
        for (int index = 0; index < 53; index++) {
            jdbc.update("INSERT INTO ai_schema.ai_conversations(id,user_id,title) VALUES (?,?,?)",
                    UUID.randomUUID(), user, "Export test");
        }
        jdbc.update("INSERT INTO ai_schema.ai_conversations(id,user_id,title) VALUES (?,?,?)",
                UUID.randomUUID(), UUID.randomUUID(), "Other user");
        assertThat(conversations.exportAll(user)).hasSize(53)
                .allSatisfy(item -> assertThat(item.title()).isEqualTo("Export test"));
    }

    @Test
    void failedExecutionRollsBackHistoryAndRequestKeySoRetryCanSucceed() {
        UUID user = UUID.randomUUID();
        var request = request(UUID.randomUUID(), "One");
        when(rag.chat(any())).thenThrow(new IllegalArgumentException("test failure"));
        assertThatThrownBy(() -> execution.chat(user, request)).isInstanceOf(IllegalArgumentException.class);
        assertThat(conversations.list(user)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ai_schema.ai_chat_requests WHERE user_id=?",
                Integer.class, user)).isZero();
        doReturn(new RagChatResponse("Hello", List.of())).when(rag).chat(any());
        assertThat(execution.chat(user, request).answer()).isEqualTo("Hello");
    }

    @Test
    void websocketPayloadConstraintsAreEnforcedAtSharedBoundary() {
        assertThatThrownBy(() -> execution.chat(UUID.randomUUID(),
                new RagChatRequest("Hello", -1, List.of(), null, UUID.randomUUID())))
                .isInstanceOf(ChatRequestException.class);
        verifyNoInteractions(rag);
    }

    private RagChatRequest request(UUID id, String question) {
        return new RagChatRequest(question, 5, List.of(), null, id);
    }
}
