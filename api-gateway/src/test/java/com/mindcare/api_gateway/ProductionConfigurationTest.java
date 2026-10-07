package com.mindcare.api_gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionConfigurationTest {
    private ApplicationContextRunner production(String profile) {
        return new ApplicationContextRunner()
                .withInitializer(context -> context.getEnvironment().setActiveProfiles(profile))
                .withUserConfiguration(ProductionConfiguration.class)
                .withPropertyValues("INTERNAL_SERVICE_SECRET=test-placeholder");
    }

    @Test
    void rejectsEmptyAndWhitespaceRequiredEnvironment() {
        for (String profile : new String[] {"prod", "production"}) {
            for (String key : new String[] {"INTERNAL_SERVICE_SECRET"}) {
                for (String blank : new String[] {"", " "}) {
                    production(profile).withPropertyValues(key + "=" + blank).run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure()).hasMessageContaining(key);
                    });
                }
            }
        }
    }

    @Test
    void acceptsExplicitProductionEnvironment() {
        production("production").run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void localDoesNotRequireProductionEnvironment() {
        new ApplicationContextRunner()
                .withUserConfiguration(ProductionConfiguration.class)
                .run(context -> assertThat(context).hasNotFailed());
    }
}
