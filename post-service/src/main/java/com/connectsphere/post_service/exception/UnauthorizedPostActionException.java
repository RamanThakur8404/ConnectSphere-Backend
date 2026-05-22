package com.connectsphere.post_service.exception;

public class UnauthorizedPostActionException extends RuntimeException {
    public UnauthorizedPostActionException(String message) {
        super(message);
    }
}
