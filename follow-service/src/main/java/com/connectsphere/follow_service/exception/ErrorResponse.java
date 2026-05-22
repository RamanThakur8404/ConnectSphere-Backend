package com.connectsphere.follow_service.exception;

import java.time.LocalDateTime;

// Standardised error response payload returned by the
public class ErrorResponse {

    private int statusCode;
    private String errorType;
    private String message;
    private String path;
    private LocalDateTime timestamp;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(int statusCode, String errorType, String message, String path) {
        this.statusCode = statusCode;
        this.errorType  = errorType;
        this.message    = message;
        this.path       = path;
        this.timestamp  = LocalDateTime.now();
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
