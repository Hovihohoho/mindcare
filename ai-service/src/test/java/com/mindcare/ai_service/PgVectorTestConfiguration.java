package com.mindcare.ai_service;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class PgVectorTestConfiguration {

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
