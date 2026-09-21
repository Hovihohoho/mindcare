package com.mindcare.ai_service.exception;

import org.springframework.http.HttpStatus;

public class ChatRequestException extends RuntimeException {
    private final HttpStatus status;
    public ChatRequestException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
    public HttpStatus status() { return status; }
}
