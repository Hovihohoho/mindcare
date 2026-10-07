package com.mindcare.api_gateway;

import java.util.List;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
@Profile({"prod", "production"})
public class ProductionConfiguration {
    @Bean
    static BeanFactoryPostProcessor requiredProductionEnvironment(Environment environment) {
        return beanFactory -> {
            for (String key : List.of("INTERNAL_SERVICE_SECRET")) {
                String value = environment.getProperty(key);
                if (value == null || value.isBlank()) {
                    throw new IllegalStateException("Production configuration requires nonblank " + key);
                }
            }
        };
    }
}
