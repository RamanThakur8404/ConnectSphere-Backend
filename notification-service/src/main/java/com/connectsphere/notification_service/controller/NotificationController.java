package com.connectsphere.notification_service.controller;

import com.connectsphere.notification_service.dto.*;
import com.connectsphere.notification_service.service.NotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// REST controller for notification management.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationController {

	private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

	private final NotificationService notificationService;

	// -----------------------------------------------------------------------
	// Creation
	// -----------------------------------------------------------------------

	// Create a notification — ADMIN or MODERATOR only (internal / system call). 
	@Operation(summary = "Create a notification", security = @SecurityRequirement(name = "bearerAuth"))
	@PostMapping
	@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
	public ResponseEntity<ApiResponse<?>> create(@Valid @RequestBody CreateRequest request) {
		log.info("POST /notifications — recipientId: {}, type: {}", request.getRecipientId(), request.getType());
		notificationService.createNotification(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(true, "Notification created", null));
	}

	// Send bulk notifications — ADMIN or MODERATOR only. 
	@Operation(summary = "Send bulk notifications", security = @SecurityRequirement(name = "bearerAuth"))
	@PostMapping("/bulk")
	@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
	public ResponseEntity<ApiResponse<?>> sendBulk(@Valid @RequestBody BulkRequest request) {
		int createdCount = notificationService.sendBulkNotification(
				request.getRecipientIds(),
				request.getActorId(),
				request.getType(),
				request.getMessage());
		return ResponseEntity.ok(new ApiResponse<>(true, "Bulk notifications sent", createdCount));
	}

	// Send an email alert — ADMIN or MODERATOR only. 
	@Operation(summary = "Send email alert", security = @SecurityRequirement(name = "bearerAuth"))
	@PostMapping("/email")
	@PreAuthorize("hasAnyRole('ADMIN', 'MODERATOR')")
	public ResponseEntity<ApiResponse<?>> sendEmail(@Valid @RequestBody EmailAlertRequest request) {
		notificationService.sendEmailAlert(request.getToEmail(), request.getSubject(), request.getBody());
		return ResponseEntity.ok(new ApiResponse<>(true, "Email sent successfully", null));
	}

	// -----------------------------------------------------------------------
	// Retrieval
	// -----------------------------------------------------------------------

	// Get notifications for a recipient. Supports {@code ?unreadOnly=true} filter
	@Operation(summary = "Get notifications for a user (optional unreadOnly filter)", security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping("/recipient/{recipientId}")
	public ResponseEntity<ApiResponse<List<ResponseDTO>>> getByRecipient(@PathVariable int recipientId,
			@RequestParam(required = false, defaultValue = "false") boolean unreadOnly,
			@RequestHeader(value = "X-User-Id", required = false) String requestUserId) {

		log.debug("GET /recipient/{} — unreadOnly: {}, X-User-Id: {}", recipientId, unreadOnly, requestUserId);
		List<ResponseDTO> data = unreadOnly ? notificationService.getUnreadByRecipient(recipientId)
				: notificationService.getByRecipient(recipientId);
		return ResponseEntity.ok(new ApiResponse<>(true, "Success", data));
	}

	// Get paginated notifications for a recipient. 
	@Operation(summary = "Get paginated notifications", security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping("/recipient/{recipientId}/paged")
	public ResponseEntity<ApiResponse<List<SummaryDTO>>> getByRecipientPaged(@PathVariable int recipientId,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {

		List<SummaryDTO> data = notificationService.getByRecipientPaged(recipientId, page, size);
		return ResponseEntity.ok(new ApiResponse<>(true, "Success", data));
	}

	// Get unread notification count for a recipient. 
	@Operation(summary = "Get unread count", security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping("/recipient/{recipientId}/unread-count")
	public ResponseEntity<ApiResponse<Integer>> getUnreadCount(@PathVariable int recipientId) {
		int count = notificationService.getUnreadCount(recipientId);
		return ResponseEntity.ok(new ApiResponse<>(true, "Success", count));
	}

	// Get all notifications — ADMIN only. 
	@Operation(summary = "Get all notifications (admin only)", security = @SecurityRequirement(name = "bearerAuth"))
	@GetMapping("/all")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<List<ResponseDTO>>> getAll() {
		List<ResponseDTO> data = notificationService.getAll();
		return ResponseEntity.ok(new ApiResponse<>(true, "Success", data));
	}

	// -----------------------------------------------------------------------
	// Read state mutations
	// -----------------------------------------------------------------------

	// Mark a single notification as read. 
	@Operation(summary = "Mark notification as read", security = @SecurityRequirement(name = "bearerAuth"))
	@PutMapping("/{notificationId}/read")
	public ResponseEntity<ApiResponse<?>> markAsRead(@PathVariable int notificationId) {
		notificationService.markAsRead(notificationId);
		return ResponseEntity.ok(new ApiResponse<>(true, "Marked as read", null));
	}

	// Mark all notifications for a recipient as read. 
	@Operation(summary = "Mark all notifications as read", security = @SecurityRequirement(name = "bearerAuth"))
	@PutMapping("/recipient/{recipientId}/read-all")
	public ResponseEntity<ApiResponse<Integer>> markAllRead(@PathVariable int recipientId) {
		int updated = notificationService.markAllRead(recipientId);
		return ResponseEntity.ok(new ApiResponse<>(true, "All marked as read", updated));
	}

	// -----------------------------------------------------------------------
	// Delete
	// -----------------------------------------------------------------------

	// Delete a notification by ID. 
	@Operation(summary = "Delete a notification", security = @SecurityRequirement(name = "bearerAuth"))
	@DeleteMapping("/{notificationId}")
	public ResponseEntity<ApiResponse<?>> delete(@PathVariable int notificationId) {
		notificationService.deleteNotification(notificationId);
		return ResponseEntity.ok(new ApiResponse<>(true, "Deleted successfully", null));
	}

	// Admin override delete — bypasses ownership check (merged from base version).
	@Operation(summary = "Admin delete (no ownership check)", security = @SecurityRequirement(name = "bearerAuth"))
	@DeleteMapping("/admin/{notificationId}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<?>> adminDelete(@PathVariable int notificationId) {
		log.info("Admin delete — notificationId: {}", notificationId);
		notificationService.deleteNotification(notificationId);
		return ResponseEntity.ok(new ApiResponse<>(true, "Deleted by admin", null));
	}
}
