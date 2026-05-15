package com.connectsphere.post_service.exception;

public class DuplicateBookmarkException extends RuntimeException {
    public DuplicateBookmarkException(String message) {
        super(message);
    }
}
