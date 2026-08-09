package com.mindcare.auth_service.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mindcare.auth_service.entity.Role;
import com.mindcare.auth_service.entity.User;
import com.mindcare.auth_service.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class PublicExpertControllerTest {

    @Test
    void listExcludesApprovedAccountsThatAreNotActuallyPublishable() {
        UserRepository repository = mock(UserRepository.class);
        User valid = expert(true, true, "ROLE_EXPERT");
        User inactive = expert(false, true, "ROLE_EXPERT");
        User unverified = expert(true, false, "ROLE_EXPERT");
        User demoted = expert(true, true, "ROLE_USER");
        when(repository.findByExpertStatus(org.mockito.ArgumentMatchers.eq("APPROVED"),
                any(Pageable.class))).thenReturn(new PageImpl<>(
                        List.of(valid, inactive, unverified, demoted)));

        var result = new PublicExpertController(repository).list(100).data();

        assertThat(result).extracting(item -> item.id()).containsExactly(valid.getId());
    }

    private User expert(boolean active, boolean verified, String roleName) {
        Role role = new Role();
        role.setName(roleName);
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFullName("Expert");
        user.setEmail(UUID.randomUUID() + "@mindcare.vn");
        user.setRole(role);
        user.setIsActive(active);
        user.setEmailVerified(verified);
        user.setExpertStatus("APPROVED");
        return user;
    }
}
