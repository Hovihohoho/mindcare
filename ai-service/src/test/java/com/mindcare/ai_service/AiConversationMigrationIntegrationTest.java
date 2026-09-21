package com.mindcare.ai_service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@Import(PgVectorTestConfiguration.class)
class AiConversationMigrationIntegrationTest {
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void flywayCreatesConversationHistoryAtVersionSix() {
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
        assertThat(version).isEqualTo("6");
    }
}
