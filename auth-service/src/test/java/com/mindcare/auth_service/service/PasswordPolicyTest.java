package com.mindcare.auth_service.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {
    private final PasswordPolicy policy = new PasswordPolicy();

    @Test
    void acceptsPasswordWithLettersAndDigits() {
        assertThatCode(() -> policy.validate("MindCare2026")).doesNotThrowAnyException();
    }

    @Test
    void rejectsWeakPasswords() {
        assertThatThrownBy(() -> policy.validate("abcdefgh")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> policy.validate("12345678")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> policy.validate("Abc123")).isInstanceOf(RuntimeException.class);
    }
}
