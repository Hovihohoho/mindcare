package com.mindcare.ai_service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@Import(AiConversationMigrationIntegrationTest.ContainerConfiguration.class)
class AiConversationMigrationIntegrationTest {
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void flywayCreatesConversationHistoryAndRequestDeduplication() {
        Integer tables = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = 'ai_schema'
                  AND table_name IN ('ai_conversations', 'ai_conversation_messages')
                """, Integer.class);
        String version = jdbcTemplate.queryForObject("""
                SELECT version FROM ai_schema.flyway_schema_history
                WHERE success ORDER BY installed_rank DESC LIMIT 1
                """, String.class);

        assertThat(tables).isEqualTo(2);
        assertThat(version).isEqualTo("7");
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='ai_schema'
                AND table_name IN ('ai_chat_requests', 'ai_chat_rate_limits')
                """, Integer.class)).isEqualTo(2);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ContainerConfiguration {
        @Bean
        @ServiceConnection
        PostgreSQLContainer postgres() {
            DockerImageName image = DockerImageName.parse("ankane/pgvector:v0.5.1")
                    .asCompatibleSubstituteFor("postgres");
            return new PostgreSQLContainer(image)
                    .withDatabaseName("ai_service_integration")
                    .withUsername("ai_test")
                    .withPassword("ai_test")
                    .withInitScript("init-pgvector.sql");
        }
    }
}
