package com.mindcare.emotionservice.shared.exception;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resourceName) {
        super("RESOURCE_NOT_FOUND", resourceName + " was not found");
    }
}
