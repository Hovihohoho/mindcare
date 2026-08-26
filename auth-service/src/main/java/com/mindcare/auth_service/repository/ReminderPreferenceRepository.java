package com.mindcare.auth_service.repository;

import com.mindcare.auth_service.entity.ReminderPreference;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderPreferenceRepository extends JpaRepository<ReminderPreference, UUID> {
    List<ReminderPreference> findByUserIdOrderByReminderType(UUID userId);
    Optional<ReminderPreference> findByUserIdAndReminderType(UUID userId, String reminderType);
    List<ReminderPreference> findByEnabledTrue();
}
