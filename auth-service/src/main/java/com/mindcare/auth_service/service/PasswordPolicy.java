package com.mindcare.auth_service.service;

import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {
    public void validate(String password) {
        if (password == null || password.length() < 8 || password.length() > 72
                || password.chars().noneMatch(Character::isLetter)
                || password.chars().noneMatch(Character::isDigit)) {
            throw new RuntimeException("Mật khẩu phải dài 8-72 ký tự và có ít nhất một chữ cái, một chữ số");
        }
    }
}
