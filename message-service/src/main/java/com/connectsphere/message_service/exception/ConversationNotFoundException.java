package com.connectsphere.message_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ConversationNotFoundException extends RuntimeException {
    public ConversationNotFoundException(Long conversationId) {
        super("Conversation not found with id: " + conversationId);
    }
}
