package com.mindcare.auth_service.support;

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
                .withDatabaseName("auth_service_integration")
                .withUsername("auth_test")
                .withPassword("auth_test");
    }
}
