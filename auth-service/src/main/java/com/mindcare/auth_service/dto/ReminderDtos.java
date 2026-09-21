package com.mindcare.auth_service.dto;

import com.mindcare.auth_service.entity.ReminderPreference;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import java.util.UUID;

public final class ReminderDtos {
    private ReminderDtos() {}
    public record Item(UUID id, String reminderType, boolean enabled, LocalTime localTime, String timezone) {
        public static Item from(ReminderPreference value) { return new Item(value.getId(), value.getReminderType(), value.isEnabled(), value.getLocalTime(), value.getTimezone()); }
    }
    public record Update(@NotNull Boolean enabled, @NotNull LocalTime localTime, @NotBlank String timezone) {}
}
