package com.mindcare.emotionservice.shared.exception;

public class ResourceConflictException extends BusinessException {

    public ResourceConflictException(String code, String message) {
        super(code, message);
    }
}
