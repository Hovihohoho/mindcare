package com.mindcare.bookingservice.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = "booking.maintenance.fixed-delay=3600000")
@Import(PostgreSqlTestcontainersConfiguration.class)
public abstract class AbstractPostgreSqlIntegrationTest {
}
