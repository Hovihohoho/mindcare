package com.mindcare.emotionservice.privacy;

import static org.assertj.core.api.Assertions.assertThat;

import com.mindcare.emotionservice.privacy.repository.UserDataRepository;
import com.mindcare.emotionservice.support.AbstractPostgreSqlIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserDataRepositoryIntegrationTest extends AbstractPostgreSqlIntegrationTest {
    @Autowired UserDataRepository repository;

    @Test
    void exportAndDeleteAreSafeForUserWithoutData() {
        UUID userId = UUID.randomUUID();

        var exported = repository.export(userId);
        var deleted = repository.delete(userId);

        assertThat(exported).containsKeys(
                "emotionJournals", "healthMetrics", "healthSourceConsents",
                "assessmentResults", "riskAlerts", "selfCarePlans",
                "selfCareActivities", "selfCareCompletions");
        assertThat(exported.values()).allMatch(java.util.List::isEmpty);
        assertThat(deleted.values()).allMatch(count -> count == 0);
    }
}
