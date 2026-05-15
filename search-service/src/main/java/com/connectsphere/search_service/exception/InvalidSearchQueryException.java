package com.connectsphere.search_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Thrown when a search query or hashtag input fails validation.
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidSearchQueryException extends RuntimeException {
    public InvalidSearchQueryException(String message) {
        super(message);
    }
}
