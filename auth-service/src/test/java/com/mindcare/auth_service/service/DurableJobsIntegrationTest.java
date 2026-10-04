package com.mindcare.auth_service.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.mindcare.auth_service.dto.AuthRequest;
import com.mindcare.auth_service.entity.Notification;
import com.mindcare.auth_service.entity.User;
import com.mindcare.auth_service.notification.ExpoPushSender;
import com.mindcare.auth_service.notification.NotificationDeliveryService;
import com.mindcare.auth_service.repository.*;
import com.mindcare.auth_service.support.AbstractPostgreSqlIntegrationTest;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class DurableJobsIntegrationTest extends AbstractPostgreSqlIntegrationTest {
    private static final AtomicInteger emotionCalls = new AtomicInteger();
    private static final AtomicInteger aiCalls = new AtomicInteger();
    private static final AtomicInteger aiStatus = new AtomicInteger(503);
    private static final HttpServer downstream = server();
    @Autowired AccountDeletionService deletion;
    @Autowired NotificationDeliveryService delivery;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired NotificationRepository notifications;
    @Autowired PasswordEncoder passwords;
    @Autowired AuthService auth;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @MockitoBean ExpoPushSender push;
    private User user;

    @DynamicPropertySource
    static void services(DynamicPropertyRegistry properties) {
        String url = "http://127.0.0.1:" + downstream.getAddress().getPort();
        properties.add("app.emotion-service-url", () -> url);
        properties.add("app.ai-service-url", () -> url);
    }

    private static HttpServer server() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", request -> {
                boolean trusted = "mindcare-test-internal-secret-32-characters".equals(request.getRequestHeaders().getFirst("X-Internal-Secret"));
                int status = 401;
                if (trusted && "DELETE".equals(request.getRequestMethod())) {
                    if (request.getRequestURI().getPath().equals("/api/v1/privacy/data")) {
                        emotionCalls.incrementAndGet(); status = 204;
                    } else if (request.getRequestURI().getPath().startsWith("/api/ai/internal/privacy/")) {
                        aiCalls.incrementAndGet(); status = aiStatus.get();
                    }
                }
                request.sendResponseHeaders(status, -1);
                request.close();
            });
            server.start();
            return server;
        } catch (java.io.IOException error) { throw new ExceptionInInitializerError(error); }
    }

    @BeforeEach
    void createUser() {
        emotionCalls.set(0); aiCalls.set(0); aiStatus.set(503);
        user = new User();
        user.setEmail(UUID.randomUUID() + "@mindcare.test");
        user.setFullName("Erasure test");
        user.setPassword(passwords.encode("StrongTest123"));
        user.setEmailVerified(true);
        user.setRole(roles.findByName("ROLE_USER").orElseThrow());
        user = users.saveAndFlush(user);
    }

    @AfterEach
    void cleanup() { if (users.existsById(user.getId())) users.deleteById(user.getId()); }
    @AfterAll
    static void stop() { downstream.stop(0); }

    @Test
    void acceptedDeletionRevokesAccessAndRetriesOnlyUnfinishedServices() throws Exception {
        var login = new AuthRequest.Login();
        login.setEmail(user.getEmail()); login.setPassword("StrongTest123");
        String token = auth.login(login, "integration-test", "127.0.0.1").accessToken();
        mvc.perform(delete("/api/auth/me/permanent").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"currentPassword\":\"StrongTest123\"}"))
                .andExpect(status().isAccepted());
        assertThat(users.findById(user.getId()).orElseThrow().getIsActive()).isFalse();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        due();
        deletion.process(user.getId());
        assertThat(users.existsById(user.getId())).isTrue();
        assertThat(emotionCalls.get()).isEqualTo(1);
        assertThat(aiCalls.get()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT emotion_deleted FROM auth_schema.account_deletion_jobs WHERE user_id=?", Boolean.class, user.getId())).isTrue();
        aiStatus.set(204);
        due();
        deletion.process(user.getId());
        assertThat(users.existsById(user.getId())).isFalse();
        assertThat(emotionCalls.get()).isEqualTo(1);
        assertThat(aiCalls.get()).isEqualTo(2);
        deletion.process(user.getId());
        assertThat(aiCalls.get()).isEqualTo(2);
    }

    @Test
    void wrongPasswordDoesNotQueueDeletion() throws Exception {
        var login = new AuthRequest.Login(); login.setEmail(user.getEmail()); login.setPassword("StrongTest123");
        String token = auth.login(login, "integration-test", "127.0.0.1").accessToken();
        mvc.perform(delete("/api/auth/me/permanent").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"currentPassword\":\"wrong\"}"))
                .andExpect(status().is4xxClientError());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM auth_schema.account_deletion_jobs WHERE user_id=?", Integer.class, user.getId())).isZero();
        assertThat(users.findById(user.getId()).orElseThrow().getIsActive()).isTrue();
    }

    @Test
    void pushFailureLeavesDurableJobForRetry() {
        var notification = notifications.saveAndFlush(Notification.create(user.getId(), UUID.randomUUID(), "SYSTEM", "Title", "Body", "/care-plan"));
        delivery.enqueue(notification, true);
        doThrow(new IllegalStateException("offline")).doNothing().when(push).send(any(), anyString(), anyString(), anyString(), anyString());
        delivery.deliver(notification.getId());
        assertThat(jdbc.queryForObject("SELECT attempts FROM auth_schema.notification_delivery_jobs WHERE notification_id=?", Integer.class, notification.getId())).isEqualTo(1);
        jdbc.update("UPDATE auth_schema.notification_delivery_jobs SET next_attempt_at=CURRENT_TIMESTAMP WHERE notification_id=?", notification.getId());
        delivery.deliver(notification.getId());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM auth_schema.notification_delivery_jobs WHERE notification_id=?", Integer.class, notification.getId())).isZero();
        assertThat(notifications.existsById(notification.getId())).isTrue();
        verify(push, times(2)).send(any(), anyString(), anyString(), anyString(), anyString());
    }

    private void due() {
        jdbc.update("UPDATE auth_schema.account_deletion_jobs SET next_attempt_at=CURRENT_TIMESTAMP, lease_until=NULL WHERE user_id=?", user.getId());
    }
}
