package com.mindcare.auth_service.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "security.jwt.secret=TWluZENhcmUtVGVzdC1Pbmx5LUpXVC1TZWNyZXQtMjAyNiE=",
        "app.internal-secret=mindcare-test-internal"
})
@Import(PostgreSqlTestcontainersConfiguration.class)
public abstract class AbstractPostgreSqlIntegrationTest {
}
