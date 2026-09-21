package com.mindcare.auth_service.entity;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReminderPreferenceTest {
    @Test void isDueOnlyInsideFiveMinuteWindow() {
        var value = ReminderPreference.create(UUID.randomUUID(), "DAILY_CHECK_IN");
        value.update(true, LocalTime.of(20, 0), "Asia/Ho_Chi_Minh");
        assertThat(value.isDue(ZonedDateTime.of(2026, 8, 26, 20, 2, 0, 0, ZoneId.of("Asia/Ho_Chi_Minh")).toInstant())).isTrue();
        assertThat(value.isDue(ZonedDateTime.of(2026, 8, 26, 20, 5, 0, 0, ZoneId.of("Asia/Ho_Chi_Minh")).toInstant())).isFalse();
    }
    @Test void supportsReminderWindowAcrossMidnight() {
        var value = ReminderPreference.create(UUID.randomUUID(), "SELF_CARE");
        value.update(true, LocalTime.of(23, 58), "Asia/Ho_Chi_Minh");
        assertThat(value.isDue(ZonedDateTime.of(2026, 8, 26, 23, 59, 0, 0, ZoneId.of("Asia/Ho_Chi_Minh")).toInstant())).isTrue();
    }
}
