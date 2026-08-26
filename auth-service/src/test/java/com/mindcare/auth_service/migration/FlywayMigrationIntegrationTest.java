package com.mindcare.auth_service.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.mindcare.auth_service.support.AbstractPostgreSqlIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class FlywayMigrationIntegrationTest extends AbstractPostgreSqlIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void appliesAllAuthMigrationsAndCreatesEngagementSchema() {
        String version = jdbcTemplate.queryForObject(
                "SELECT version FROM auth_schema.flyway_schema_history "
                        + "WHERE success ORDER BY installed_rank DESC LIMIT 1",
                String.class);
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables "
                        + "WHERE table_schema = 'auth_schema' AND table_type = 'BASE TABLE'",
                String.class);

        assertThat(version).isEqualTo("7");
        assertThat(tables).contains("users", "user_sessions", "notifications", "bookmarks");
    }

    @Test
    void notificationSchemaSupportsEventsAndReadState() {
        List<String> columns = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns "
                        + "WHERE table_schema = 'auth_schema' AND table_name = 'notifications'",
                String.class);
        Integer uniqueEventIndexes = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'auth_schema' "
                        + "AND indexname = 'uq_notifications_source_user'",
                Integer.class);

        assertThat(columns).contains("user_id", "source_event_id", "type", "title", "message",
                "action_url", "read_at", "created_at");
        assertThat(columns).doesNotContain("content", "notification_type");
        assertThat(uniqueEventIndexes).isOne();
    }
}
