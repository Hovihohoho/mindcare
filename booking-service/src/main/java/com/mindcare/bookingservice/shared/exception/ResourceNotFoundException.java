package com.mindcare.bookingservice.shared.exception;

import org.springframework.http.HttpStatus;

public final class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException() {
        super("RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, "The requested resource was not found");
    }
}
