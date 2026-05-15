package com.connectsphere.search_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Thrown when a requested hashtag does not exist in the system.
@ResponseStatus(HttpStatus.NOT_FOUND)
public class HashtagNotFoundException extends RuntimeException {
    public HashtagNotFoundException(String message) {
        super(message);
    }
}
