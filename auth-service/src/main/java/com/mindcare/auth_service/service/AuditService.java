package com.mindcare.auth_service.service;

import com.mindcare.auth_service.entity.AdminAuditLog;
import com.mindcare.auth_service.repository.AdminAuditLogRepository;
import com.mindcare.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AdminAuditLogRepository repository;
    private final UserRepository userRepository;

    public void record(String adminEmail, String action, String targetType, String targetId, String detail) {
        AdminAuditLog log = new AdminAuditLog();
        log.setAdmin(userRepository.findByEmail(adminEmail).orElse(null));
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        repository.save(log);
    }
}
