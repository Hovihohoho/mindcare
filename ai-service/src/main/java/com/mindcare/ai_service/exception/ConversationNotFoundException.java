package com.mindcare.ai_service.exception;

public class ConversationNotFoundException extends RuntimeException {
    public ConversationNotFoundException() {
        super("Không tìm thấy cuộc trò chuyện.");
    }
}
