package com.connectsphere.follow_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Thrown when a requested follow relationship does not exist in the database.
@ResponseStatus(HttpStatus.NOT_FOUND)
public class FollowNotFoundException extends RuntimeException {

    public FollowNotFoundException(String message) {
        super(message);
    }

    public FollowNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
