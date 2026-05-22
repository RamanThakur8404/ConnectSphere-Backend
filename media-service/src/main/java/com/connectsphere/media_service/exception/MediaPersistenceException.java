package com.connectsphere.media_service.exception;

import org.springframework.http.HttpStatus;

// Thrown when a database operation on a media or story record fails
public class MediaPersistenceException extends MediaServiceException {

    public MediaPersistenceException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}