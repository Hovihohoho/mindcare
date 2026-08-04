package com.mindcare.auth_service.repository;

import com.mindcare.auth_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<User> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String fullName, String email, Pageable pageable);

    Page<User> findByExpertStatus(String expertStatus, Pageable pageable);

    long countByIsActiveTrue();
    long countByRoleName(String roleName);
    long countByExpertStatus(String expertStatus);

    List<User> findByRoleNameAndIsActiveTrueOrderByFullNameAsc(String roleName);
}
