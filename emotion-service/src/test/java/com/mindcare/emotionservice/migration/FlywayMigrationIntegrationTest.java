package com.mindcare.emotionservice.migration;

import com.mindcare.emotionservice.support.AbstractPostgreSqlIntegrationTest;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationIntegrationTest extends AbstractPostgreSqlIntegrationTest {

    private static final Set<String> BUSINESS_TABLES = Set.of(
            "emotion_journals",
            "health_metrics",
            "health_metric_sync_requests",
            "assessments",
            "questions",
            "answer_options",
            "assessment_results",
            "psychological_alert_logs"
    );

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PostgreSQLContainer postgreSqlContainer;

    @Test
    void flywayAppliesAllMigrationsOnEmptyPostgreSql() {
        String currentVersion = jdbcTemplate.queryForObject(
                """
                SELECT version
                FROM emotion_schema.flyway_schema_history
                WHERE success
                ORDER BY installed_rank DESC
                LIMIT 1
                """,
                String.class
        );
        assertThat(currentVersion).isEqualTo("10");
        assertThat(flyway.info().pending()).isEmpty();

        List<String> tables = jdbcTemplate.queryForList(
                """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'emotion_schema'
                  AND table_type = 'BASE TABLE'
                """,
                String.class
        );
        assertThat(tables).containsAll(BUSINESS_TABLES);
    }

    @Test
    void schemaUsesApplicationUuidAndTimestamptzRules() {
        Integer uuidDefaults = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = 'emotion_schema'
                  AND column_name = 'id'
                  AND column_default IS NOT NULL
                """,
                Integer.class
        );
        Integer auditDefaults = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = 'emotion_schema'
                  AND column_name IN ('created_at', 'updated_at')
                  AND column_default IS NOT NULL
                """,
                Integer.class
        );
        Integer invalidTimeColumns = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = 'emotion_schema'
                  AND (column_name LIKE '%\\_at' ESCAPE '\\' OR column_name IN ('created_at', 'updated_at'))
                  AND data_type <> 'timestamp with time zone'
                """,
                Integer.class
        );

        assertThat(uuidDefaults).isZero();
        assertThat(auditDefaults).isZero();
        assertThat(invalidTimeColumns).isZero();
    }

    @Test
    void migrationsCreateIdempotencyAndLifecycleConstraints() {
        Integer syncUniqueConstraint = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE constraint_schema = 'emotion_schema'
                  AND table_name = 'health_metric_sync_requests'
                  AND constraint_name = 'uq_health_metric_sync_request'
                  AND constraint_type = 'UNIQUE'
                """,
                Integer.class
        );
        Integer assessmentStatusConstraint = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE constraint_schema = 'emotion_schema'
                  AND table_name = 'assessments'
                  AND constraint_name = 'ck_assessment_status'
                  AND constraint_type = 'CHECK'
                """,
                Integer.class
        );
        Integer publishedIndex = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM pg_indexes
                WHERE schemaname = 'emotion_schema'
                  AND indexname = 'uq_assessment_one_published_version'
                """,
                Integer.class
        );
        Integer draftIndex = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM pg_indexes
                WHERE schemaname = 'emotion_schema'
                  AND indexname = 'uq_assessment_one_draft_version'
                """,
                Integer.class
        );

        assertThat(syncUniqueConstraint).isOne();
        assertThat(assessmentStatusConstraint).isOne();
        assertThat(publishedIndex).isOne();
        assertThat(draftIndex).isOne();
    }

    @Test
    void existingV1DataUpgradesThroughLatestVersionWithoutLoss() throws Exception {
        String databaseName = "emotion_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        jdbcTemplate.execute("CREATE DATABASE " + databaseName);
        String upgradeUrl = "jdbc:postgresql://%s:%d/%s".formatted(
                postgreSqlContainer.getHost(),
                postgreSqlContainer.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
                databaseName
        );

        Flyway v1Flyway = Flyway.configure()
                .dataSource(upgradeUrl, postgreSqlContainer.getUsername(), postgreSqlContainer.getPassword())
                .schemas("emotion_schema")
                .defaultSchema("emotion_schema")
                .locations("classpath:db/migration")
                .target(MigrationVersion.fromVersion("1"))
                .load();
        v1Flyway.migrate();

        UUID journalId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        try (var connection = DriverManager.getConnection(
                upgradeUrl,
                postgreSqlContainer.getUsername(),
                postgreSqlContainer.getPassword()
        ); var statement = connection.prepareStatement("""
                INSERT INTO emotion_schema.emotion_journals
                    (id, user_id, emotion_type, content, created_at, updated_at)
                VALUES (?, ?, 'HAPPY', 'legacy-v1', ?, ?)
                """)) {
            LocalDateTime legacyUtc = LocalDateTime.of(2026, 1, 1, 12, 0);
            statement.setObject(1, journalId);
            statement.setObject(2, userId);
            statement.setObject(3, legacyUtc);
            statement.setObject(4, legacyUtc);
            statement.executeUpdate();
        }

        Flyway latestFlyway = Flyway.configure()
                .dataSource(upgradeUrl, postgreSqlContainer.getUsername(), postgreSqlContainer.getPassword())
                .schemas("emotion_schema")
                .defaultSchema("emotion_schema")
                .locations("classpath:db/migration")
                .load();
        latestFlyway.migrate();

        try (var connection = DriverManager.getConnection(
                upgradeUrl,
                postgreSqlContainer.getUsername(),
                postgreSqlContainer.getPassword()
        ); var statement = connection.prepareStatement("""
                SELECT content,
                       created_at AT TIME ZONE 'UTC' AS created_at_utc,
                       pg_typeof(created_at)::text AS created_at_type
                FROM emotion_schema.emotion_journals
                WHERE id = ?
                """)) {
            statement.setObject(1, journalId);
            try (var result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString("content")).isEqualTo("legacy-v1");
                assertThat(result.getObject("created_at_utc", LocalDateTime.class))
                        .isEqualTo(LocalDateTime.of(2026, 1, 1, 12, 0));
                assertThat(result.getString("created_at_type")).isEqualTo("timestamp with time zone");
            }
        }

        try (var connection = DriverManager.getConnection(
                upgradeUrl,
                postgreSqlContainer.getUsername(),
                postgreSqlContainer.getPassword()
        ); var statement = connection.prepareStatement("""
                SELECT version
                FROM emotion_schema.flyway_schema_history
                WHERE success
                ORDER BY installed_rank DESC
                LIMIT 1
                """);
             var result = statement.executeQuery()) {
            assertThat(result.next()).isTrue();
            assertThat(result.getString("version")).isEqualTo("10");
        }
    }
}
