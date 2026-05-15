package com.connectsphere.like_service.exception;

import com.connectsphere.like_service.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

// Centralised exception handler for the Like Service.
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // -----------------------------------------------------------------------
    // Domain exceptions
    // -----------------------------------------------------------------------

    // Handles reactions that cannot be found.
    @ExceptionHandler(LikeNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleLikeNotFound(LikeNotFoundException ex) {
        logger.error("LikeNotFoundException: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // Handles duplicate reaction attempts (one-reaction-per-user rule violation).
    @ExceptionHandler(DuplicateLikeException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateLike(DuplicateLikeException ex) {
        logger.error("DuplicateLikeException: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // Handles invalid target-type values (not POST or COMMENT).
    @ExceptionHandler(InvalidTargetTypeException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidTargetType(
            InvalidTargetTypeException ex) {
        logger.error("InvalidTargetTypeException: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    // -----------------------------------------------------------------------
    // Validation exceptions
    // -----------------------------------------------------------------------

    // Handles Bean Validation failures (e.g. @NotNull, @Pattern).
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        logger.error("Validation errors: {}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("Validation failed")
                        .data(errors)
                        .timestamp(java.time.LocalDateTime.now())
                        .build());
    }

    // -----------------------------------------------------------------------
    // Database exceptions
    // -----------------------------------------------------------------------

    // Handles database unique-constraint violations (e.g. duplicate like at DB level).
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex) {
        logger.error("DataIntegrityViolationException: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("Data integrity violation: reaction may already exist"));
    }

    // -----------------------------------------------------------------------
    // Fallback
    // -----------------------------------------------------------------------

    // Catches any unhandled exception and returns a generic 500 response.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        logger.error("Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Please try again later."));
    }
}
