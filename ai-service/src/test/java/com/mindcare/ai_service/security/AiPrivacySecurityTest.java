package com.mindcare.ai_service.security;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.mindcare.ai_service.controller.AiPrivacyController;
import com.mindcare.ai_service.service.AiConversationService;
import jakarta.servlet.Filter;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

class AiPrivacySecurityTest {
    @Test
    void authenticatedUsersCanExportAndDeleteOnlyTheirOwnData() throws Exception {
        try (var context = new AnnotationConfigWebApplicationContext()) {
            context.setServletContext(new MockServletContext());
            context.register(Config.class);
            context.refresh();
            var mvc = MockMvcBuilders.webAppContextSetup(context)
                    .addFilters(context.getBean("springSecurityFilterChain", Filter.class)).build();
            var service = context.getBean(AiConversationService.class);
            UUID user = UUID.randomUUID();
            when(service.exportAll(user)).thenReturn(List.of());
            mvc.perform(get("/api/ai/privacy/export")).andExpect(status().isUnauthorized());
            mvc.perform(delete("/api/ai/privacy/data")).andExpect(status().isUnauthorized());
            mvc.perform(get("/api/ai/privacy/export").header("X-User-Id", user.toString()))
                    .andExpect(status().isUnauthorized());
            mvc.perform(get("/api/ai/privacy/export").header("X-User-Id", user.toString())
                    .header("X-User-Role", "ROLE_USER")).andExpect(status().isOk());
            mvc.perform(delete("/api/ai/privacy/data").header("X-User-Id", user.toString())
                    .header("X-User-Role", "ROLE_USER")).andExpect(status().isOk());
            verify(service).exportAll(user);
            verify(service).deleteAll(user);
            verifyNoMoreInteractions(service);
        }
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, GatewayAuthenticationFilter.class, AiPrivacyController.class})
    static class Config {
        @Bean AiConversationService conversations() { return mock(AiConversationService.class); }
    }
}
