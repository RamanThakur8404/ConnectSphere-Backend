package com.connectsphere.follow_service.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

// Centralised exception handler for the Follow-Service.
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // -------------------------------------------------------------------------
    // Application-specific exceptions
    // -------------------------------------------------------------------------

    // Handles {@link FollowNotFoundException} – 404 Not Found.
    @ExceptionHandler(FollowNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFollowNotFound(
            FollowNotFoundException ex, HttpServletRequest request) {

        LOGGER.error("FollowNotFoundException at {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Follow Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // Handles {@link DuplicateFollowException} – 409 Conflict.
    @ExceptionHandler(DuplicateFollowException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateFollow(
            DuplicateFollowException ex, HttpServletRequest request) {

        LOGGER.error("DuplicateFollowException at {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                "Duplicate Follow",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // Handles {@link SelfFollowException} – 400 Bad Request.
    @ExceptionHandler(SelfFollowException.class)
    public ResponseEntity<ErrorResponse> handleSelfFollow(
            SelfFollowException ex, HttpServletRequest request) {

        LOGGER.error("SelfFollowException at {}: {}", request.getRequestURI(), ex.getMessage());
        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Self Follow Not Allowed",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // -------------------------------------------------------------------------
    // Validation exceptions
    // -------------------------------------------------------------------------

    // Handles Bean Validation failures on @RequestBody fields – 400 Bad Request.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        String errorMessage = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));

        LOGGER.warn("Validation failed at {}: {}", request.getRequestURI(), errorMessage);
        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation Error",
                errorMessage,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // Handles constraint violations on @PathVariable / @RequestParam – 400 Bad Request.
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {

        String errorMessage = ex.getConstraintViolations()
                .stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.joining("; "));

        LOGGER.warn("Constraint violation at {}: {}", request.getRequestURI(), errorMessage);
        ErrorResponse response = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Constraint Violation",
                errorMessage,
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // -------------------------------------------------------------------------
    // Fallback
    // -------------------------------------------------------------------------

    // Catch-all handler for any unhandled exception – 500 Internal Server Error.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        LOGGER.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        ErrorResponse response = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
