package com.mindcare.ai_service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@AutoConfigureMockMvc
@Import(PgVectorTestConfiguration.class)
class AiServiceApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void acceptsIdentityHeadersVerifiedByGateway() throws Exception {
        mockMvc.perform(get("/api/ai/self-care-content")
                        .header("X-User-Id", "41aa1147-d62c-46f9-ae3f-83fcb76aafa7")
                        .header("X-User-Role", "ROLE_USER"))
                .andExpect(status().isOk());
    }
}
