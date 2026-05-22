package com.connectsphere.notification_service.exception;

import com.connectsphere.notification_service.dto.ApiResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

// Centralized exception handler for the Notification Service.
@RestControllerAdvice
public class GlobalExceptionHandler {

	//  Domain exceptions ────────────────────────────────────────────────────

	// Handles {@link NotificationNotFoundException} → 404 Not Found.
	@ExceptionHandler(NotificationNotFoundException.class)
	public ResponseEntity<ApiResponse<?>> handleNotFound(NotificationNotFoundException ex) {
		return build(ex.getStatus(), ex.getMessage());
	}

	// Handles {@link InvalidNotificationRequestException} → 400 Bad Request.
	@ExceptionHandler(InvalidNotificationRequestException.class)
	public ResponseEntity<ApiResponse<?>> handleInvalidRequest(InvalidNotificationRequestException ex) {
		return build(ex.getStatus(), ex.getMessage());
	}

	// Handles {@link BulkNotificationException} → 400 Bad Request (or overridden
	@ExceptionHandler(BulkNotificationException.class)
	public ResponseEntity<ApiResponse<?>> handleBulk(BulkNotificationException ex) {
		return build(ex.getStatus(), ex.getMessage());
	}

	// Handles {@link EmailNotificationException} → 502 Bad Gateway (or overridden
	@ExceptionHandler(EmailNotificationException.class)
	public ResponseEntity<ApiResponse<?>> handleEmail(EmailNotificationException ex) {
		return build(ex.getStatus(), ex.getMessage());
	}

	// Handles {@link NotificationAlreadyReadException} → 409 Conflict.
	@ExceptionHandler(NotificationAlreadyReadException.class)
	public ResponseEntity<ApiResponse<?>> handleAlreadyRead(NotificationAlreadyReadException ex) {
		return build(ex.getStatus(), ex.getMessage());
	}

	// Handles {@link NotificationPersistenceException} → 500 Internal Server Error.
	@ExceptionHandler(NotificationPersistenceException.class)
	public ResponseEntity<ApiResponse<?>> handlePersistence(NotificationPersistenceException ex) {
		return build(ex.getStatus(), ex.getMessage());
	}

	// Catches the remaining base type so no {@link NotificationServiceException}
	@ExceptionHandler(NotificationServiceException.class)
	public ResponseEntity<ApiResponse<?>> handleServiceException(NotificationServiceException ex) {
		return build(ex.getStatus(), ex.getMessage());
	}

	//  Spring / Jakarta Validation ─────────────────────────────────────────

	// Handles {@code @Valid} bean-validation failures on request bodies. Collects
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<?>> handleValidation(MethodArgumentNotValidException ex) {
		String errors = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> fe.getField() + ": " + fe.getDefaultMessage()).collect(Collectors.joining("; "));
		return build(HttpStatus.BAD_REQUEST, "Validation failed: " + errors);
	}

	// Handles illegal argument errors (e.g. from service-layer guard clauses). Maps
	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponse<?>> handleIllegalArgument(IllegalArgumentException ex) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	//  Fallback ─────────────────────────────────────────────────────────────

	// Safety net for any unhandled {@link Exception}. Maps to HTTP 500 Internal
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<?>> handleGeneric(Exception ex) {
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again later.");
	}

	//  Helper ───────────────────────────────────────────────────────────────

	private ResponseEntity<ApiResponse<?>> build(HttpStatus status, String message) {
		return ResponseEntity.status(status).body(new ApiResponse<>(false, message, null));
	}
}