package com.mindcare.ai_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@org.testcontainers.junit.jupiter.Testcontainers(disabledWithoutDocker = true)
@org.springframework.context.annotation.Import(AiConversationMigrationIntegrationTest.ContainerConfiguration.class)
class AiServiceApplicationTests {
    @Test
    void contextLoads() {
    }
}
