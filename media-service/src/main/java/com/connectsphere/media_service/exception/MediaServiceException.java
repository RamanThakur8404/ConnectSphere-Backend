package com.connectsphere.media_service.exception;

import org.springframework.http.HttpStatus;

public class MediaServiceException extends RuntimeException {

    private final HttpStatus status;

    public MediaServiceException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public MediaServiceException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
