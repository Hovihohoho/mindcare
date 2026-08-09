package com.mindcare.bookingservice.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.mindcare.bookingservice.support.AbstractPostgreSqlIntegrationTest;
import java.util.List;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class FlywayMigrationIntegrationTest extends AbstractPostgreSqlIntegrationTest {

    private static final Set<String> BUSINESS_TABLES = Set.of(
            "expert_schedules",
            "bookings",
            "conversations",
            "messages",
            "expert_reviews",
            "consultation_notes",
            "payments",
            "payment_refunds",
            "payment_webhook_receipts",
            "outbox_events");

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayAppliesAllBookingMigrations() {
        String version = jdbcTemplate.queryForObject(
                """
                SELECT version
                FROM booking_schema.flyway_schema_history
                WHERE success
                ORDER BY installed_rank DESC
                LIMIT 1
                """,
                String.class);
        List<String> tables = jdbcTemplate.queryForList(
                """
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'booking_schema'
                  AND table_type = 'BASE TABLE'
                """,
                String.class);

        assertThat(version).isEqualTo("3");
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(tables).containsAll(BUSINESS_TABLES);
        assertThat(tables).doesNotContain("expert_profiles");
    }

    @Test
    void schemaUsesApplicationManagedUuidAuditAndTimestamptz() {
        Integer uuidDefaults = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = 'booking_schema'
                  AND column_name = 'id'
                  AND column_default IS NOT NULL
                """,
                Integer.class);
        Integer auditDefaults = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = 'booking_schema'
                  AND column_name IN ('created_at', 'updated_at')
                  AND column_default IS NOT NULL
                """,
                Integer.class);
        Integer invalidTimes = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = 'booking_schema'
                  AND column_name LIKE '%\\_at' ESCAPE '\\'
                  AND data_type <> 'timestamp with time zone'
                """,
                Integer.class);

        assertThat(uuidDefaults).isZero();
        assertThat(auditDefaults).isZero();
        assertThat(invalidTimes).isZero();
    }

    @Test
    void schemaContainsInstantBookingAndRefundGuards() {
        Integer holdColumn = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = 'booking_schema'
                  AND table_name = 'expert_schedules'
                  AND column_name = 'hold_expires_at'
                """,
                Integer.class);
        Integer bookingIdempotency = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE constraint_schema = 'booking_schema'
                  AND constraint_name = 'uq_bookings_user_idempotency'
                  AND constraint_type = 'UNIQUE'
                """,
                Integer.class);
        Integer refundPaymentUnique = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE constraint_schema = 'booking_schema'
                  AND constraint_name = 'uq_payment_refunds_payment'
                  AND constraint_type = 'UNIQUE'
                """,
                Integer.class);

        assertThat(holdColumn).isOne();
        assertThat(bookingIdempotency).isOne();
        assertThat(refundPaymentUnique).isOne();
    }
}
