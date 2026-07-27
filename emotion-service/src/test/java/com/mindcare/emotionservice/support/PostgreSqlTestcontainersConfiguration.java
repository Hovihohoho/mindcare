package com.mindcare.emotionservice.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class PostgreSqlTestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgreSqlContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:16.8-alpine"))
                .withDatabaseName("emotion_service_integration")
                .withUsername("emotion_test")
                .withPassword("emotion_test");
    }
}
