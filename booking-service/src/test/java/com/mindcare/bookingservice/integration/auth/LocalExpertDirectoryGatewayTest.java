package com.mindcare.bookingservice.integration.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mindcare.bookingservice.shared.exception.BusinessException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LocalExpertDirectoryGatewayTest {

    private final LocalExpertDirectoryGateway gateway =
            new LocalExpertDirectoryGateway();

    @Test
    void paginatesBookableExpertsWithoutDuplicates() {
        ExpertDirectoryPage first =
                gateway.listBookable(new ExpertDirectoryQuery(null, null, null, 2));
        ExpertDirectoryPage second =
                gateway.listBookable(
                        new ExpertDirectoryQuery(null, null, first.nextCursor(), 2));

        assertThat(first.items()).hasSize(2);
        assertThat(first.hasMore()).isTrue();
        assertThat(second.items()).hasSize(1);
        assertThat(second.hasMore()).isFalse();
        assertThat(second.items())
                .extracting(ExpertDirectoryItem::expertUserId)
                .doesNotContain(
                        first.items().get(0).expertUserId(),
                        first.items().get(1).expertUserId());
    }

    @Test
    void filtersBySpecialtyAndProvidesBookingProfile() {
        ExpertDirectoryPage page = gateway.listBookable(
                new ExpertDirectoryQuery(null, "SLEEP", null, 20));

        assertThat(page.items()).singleElement().satisfies(item -> {
            assertThat(item.expertUserId())
                    .isEqualTo(UUID.fromString(
                            "33333333-3333-4333-8333-333333333333"));
            assertThat(item.specialties()).contains("SLEEP");
        });
        ExpertBookingProfile profile =
                gateway.getBookingProfile(page.items().get(0).expertUserId());
        assertThat(profile.eligible()).isTrue();
        assertThat(profile.currency()).isEqualTo("VND");
    }

    @Test
    void rejectsMalformedCursor() {
        assertThatThrownBy(() -> gateway.listBookable(
                        new ExpertDirectoryQuery(null, null, "not-a-cursor", 20)))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("INVALID_CURSOR"));
    }
}
