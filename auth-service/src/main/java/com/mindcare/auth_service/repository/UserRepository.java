package com.mindcare.auth_service.repository;

import com.mindcare.auth_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRoleNameAndIsActiveTrueOrderByFullNameAsc(String roleName);
}
