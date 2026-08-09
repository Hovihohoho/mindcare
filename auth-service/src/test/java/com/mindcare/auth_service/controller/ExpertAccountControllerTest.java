package com.mindcare.auth_service.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mindcare.auth_service.entity.User;
import com.mindcare.auth_service.repository.ExpertDocumentRepository;
import com.mindcare.auth_service.repository.UserRepository;
import com.mindcare.auth_service.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

class ExpertAccountControllerTest {

    @Test
    void submitRejectsApplicationAlreadyPending() {
        AccountService accountService = mock(AccountService.class);
        UserRepository userRepository = mock(UserRepository.class);
        ExpertDocumentRepository documentRepository = mock(ExpertDocumentRepository.class);
        Authentication authentication = mock(Authentication.class);
        User user = new User();
        user.setExpertStatus("PENDING");
        when(authentication.getName()).thenReturn("applicant@mindcare.vn");
        when(accountService.current("applicant@mindcare.vn")).thenReturn(user);
        ExpertAccountController controller = new ExpertAccountController(
                accountService, userRepository, documentRepository);

        assertThatThrownBy(() -> controller.submit(authentication))
                .hasMessageContaining("đang chờ duyệt");
        verify(userRepository, never()).save(user);
    }
}
