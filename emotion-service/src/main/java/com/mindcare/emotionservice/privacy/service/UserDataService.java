package com.mindcare.emotionservice.privacy.service;

import com.mindcare.emotionservice.privacy.repository.UserDataRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDataService {
    private final UserDataRepository repository;

    public UserDataService(UserDataRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public UserDataExport export(UUID userId) {
        return new UserDataExport(OffsetDateTime.now(), repository.export(userId));
    }

    @Transactional
    public Map<String, Integer> delete(UUID userId) {
        return repository.delete(userId);
    }

    public record UserDataExport(
            OffsetDateTime exportedAt,
            Map<String, List<Map<String, Object>>> data) {}
}
