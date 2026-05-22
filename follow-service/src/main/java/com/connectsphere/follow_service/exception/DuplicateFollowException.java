package com.connectsphere.follow_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Thrown when a user attempts to follow another user they are already following.
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateFollowException extends RuntimeException {

    public DuplicateFollowException(String message) {
        super(message);
    }

    public DuplicateFollowException(String message, Throwable cause) {
        super(message, cause);
    }
}
