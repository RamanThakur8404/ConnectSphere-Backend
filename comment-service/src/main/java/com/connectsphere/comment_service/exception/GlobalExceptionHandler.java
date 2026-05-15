package com.connectsphere.comment_service.exception;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	// Helper method
	private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String error, String message) {
		ErrorResponse response = new ErrorResponse(status.value(), error, message);
		return ResponseEntity.status(status).body(response);
	}

	@ExceptionHandler(CommentNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleCommentNotFound(CommentNotFoundException ex) {
		logger.warn("Comment not found: {}", ex.getMessage());
		return buildResponse(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", ex.getMessage());
	}

	@ExceptionHandler(UnauthorizedCommentAccessException.class)
	public ResponseEntity<ErrorResponse> handleUnauthorizedAccess(UnauthorizedCommentAccessException ex) {
		logger.warn("Unauthorized comment access: {}", ex.getMessage());
		return buildResponse(HttpStatus.FORBIDDEN, "UNAUTHORIZED", ex.getMessage());
	}

	@ExceptionHandler(InvalidCommentOperationException.class)
	public ResponseEntity<ErrorResponse> handleInvalidOperation(InvalidCommentOperationException ex) {
		logger.warn("Invalid comment operation: {}", ex.getMessage());
		return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_OPERATION", ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
		logger.warn("Validation failed: {}", ex.getMessage());

		Map<String, String> fieldErrors = new HashMap<>();
		for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
			fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
		}

		ErrorResponse response = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR",
				"One or more fields failed validation");
		response.setFieldErrors(fieldErrors);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}
	
	@ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        logger.warn("Illegal argument: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST,"ILLEGAL_ARGUMENT",ex.getMessage());
       }

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
		logger.error("Unexpected error occurred: {}", ex.getMessage(), ex);
		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
				"An unexpected error occurred. Please try again later.");
	}
}
