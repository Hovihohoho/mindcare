package com.mindcare.emotionservice.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("integration")
@Import(PostgreSqlTestcontainersConfiguration.class)
public abstract class AbstractPostgreSqlIntegrationTest {
}
