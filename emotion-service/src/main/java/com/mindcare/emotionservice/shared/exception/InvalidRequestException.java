package com.mindcare.emotionservice.shared.exception;

public class InvalidRequestException extends BusinessException {

    public InvalidRequestException(String code, String message) {
        super(code, message);
    }
}
