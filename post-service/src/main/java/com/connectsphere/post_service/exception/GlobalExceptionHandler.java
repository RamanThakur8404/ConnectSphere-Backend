package com.connectsphere.post_service.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(PostNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handlePostNotFoundException(PostNotFoundException ex) {

		logger.error("PostNotFoundException: {}", ex.getMessage());
		return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex) {

        logger.error("Validation failed — {} field error(s)",
                ex.getBindingResult().getFieldErrorCount());

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status",    HttpStatus.BAD_REQUEST.value());
        response.put("error",     "Validation Failed");
        response.put("message",   "One or more fields failed validation. See fieldErrors.");
        response.put("fieldErrors", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {

		String message = "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'. Expected type: "
				+ (ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

		logger.error("MethodArgumentTypeMismatchException: {}", message);
		return buildErrorResponse(HttpStatus.BAD_REQUEST, message);
	}

	@ExceptionHandler(MissingRequestHeaderException.class)
	public ResponseEntity<Map<String, Object>> handleMissingRequestHeader(MissingRequestHeaderException ex) {

		String message = "Missing required request header: " + ex.getHeaderName();
		logger.error("MissingRequestHeaderException: {}", message);
		return buildErrorResponse(HttpStatus.BAD_REQUEST, message);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
		logger.error("Unexpected error in Post-Service: {}", ex.getMessage(), ex);
		return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
				"An unexpected error occurred. Please try again later.");
	}

	private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String message) {

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("timestamp", LocalDateTime.now().toString());
		response.put("status", status.value());
		response.put("error", status.getReasonPhrase());
		response.put("message", message);

		return ResponseEntity.status(status).body(response);
	}
}
