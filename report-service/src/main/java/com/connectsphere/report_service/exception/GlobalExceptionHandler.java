package com.connectsphere.report_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

// Centralised exception → HTTP response mapping for the Report-Service.
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Domain exceptions

    @ExceptionHandler(ReportNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ReportNotFoundException ex) {
        log.warn("Report not found: {}", ex.getMessage());
        return buildError(HttpStatus.NOT_FOUND, "REPORT_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(DuplicateReportException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateReportException ex) {
        log.warn("Duplicate report attempt: {}", ex.getMessage());
        return buildError(HttpStatus.CONFLICT, "DUPLICATE_REPORT", ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex) {
        return buildError(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage());
    }

    // Validation

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        log.warn("Validation failed: {}", fieldErrors);

        ValidationErrorResponse body = new ValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_FAILED",
                fieldErrors,
                LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // Catch-all

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unhandled exception: ", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR", "An unexpected error occurred");
    }

    // Response models

    private ResponseEntity<ErrorResponse> buildError(HttpStatus status, String code, String message) {
        ErrorResponse body = new ErrorResponse(status.value(), code, message, LocalDateTime.now());
        return ResponseEntity.status(status).body(body);
    }

    public record ErrorResponse(int status, String error, String message, LocalDateTime timestamp) {}

    public record ValidationErrorResponse(
            int status, String error,
            Map<String, String> fields,
            LocalDateTime timestamp) {}
}
