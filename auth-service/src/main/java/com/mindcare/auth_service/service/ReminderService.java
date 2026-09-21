package com.mindcare.auth_service.service;

import com.mindcare.auth_service.dto.ReminderDtos;
import com.mindcare.auth_service.entity.ReminderPreference;
import com.mindcare.auth_service.repository.ReminderPreferenceRepository;
import jakarta.transaction.Transactional;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class ReminderService {
    public static final Set<String> TYPES = Set.of("DAILY_CHECK_IN", "SELF_CARE");
    private final ReminderPreferenceRepository repository;
    @Transactional
    public List<ReminderDtos.Item> list(UUID userId) {
        for (String type : TYPES) repository.findByUserIdAndReminderType(userId, type).orElseGet(() -> repository.save(ReminderPreference.create(userId, type)));
        return repository.findByUserIdOrderByReminderType(userId).stream().map(ReminderDtos.Item::from).toList();
    }
    @Transactional
    public ReminderDtos.Item update(UUID userId, String type, ReminderDtos.Update request) {
        if (!TYPES.contains(type)) throw new IllegalArgumentException("Unsupported reminder type");
        var value = repository.findByUserIdAndReminderType(userId, type).orElseGet(() -> ReminderPreference.create(userId, type));
        value.update(request.enabled(), request.localTime(), request.timezone());
        return ReminderDtos.Item.from(repository.save(value));
    }
}
