package com.connectsphere.comment_service.controller;

import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.connectsphere.comment_service.dto.CommentRequestDTO;
import com.connectsphere.comment_service.dto.CommentResponseDTO;
import com.connectsphere.comment_service.dto.CommentUpdateDTO;
import com.connectsphere.comment_service.exception.UnauthorizedCommentAccessException;
import com.connectsphere.comment_service.service.CommentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// REST Controller for comment-service.
@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
public class CommentController {

	private static final Logger logger = LoggerFactory.getLogger(CommentController.class);
	private final CommentService commentService;

	// =========================================================================
	// PUBLIC — no auth required
	// =========================================================================

	@Operation(summary = "Get all comments for a post (public)")
	@GetMapping("/post/{postId}")
	public ResponseEntity<List<CommentResponseDTO>> getCommentsByPost(@PathVariable Long postId) {
		logger.info("GET /api/v1/comments/post/{}", postId);
		return ResponseEntity.ok(commentService.getCommentsByPost(postId));
	}

	@Operation(summary = "Get a single comment by ID (public)")
	@GetMapping("/{commentId}")
	public ResponseEntity<CommentResponseDTO> getCommentById(@PathVariable Long commentId) {
		logger.info("GET /api/v1/comments/{}", commentId);
		return ResponseEntity.ok(commentService.getCommentById(commentId));
	}

	@Operation(summary = "Get replies to a comment (public)")
	@GetMapping("/{commentId}/replies")
	public ResponseEntity<List<CommentResponseDTO>> getReplies(@PathVariable Long commentId) {
		logger.info("GET /api/v1/comments/{}/replies", commentId);
		return ResponseEntity.ok(commentService.getReplies(commentId));
	}

	@Operation(summary = "Get comment count for a post (public)")
	@GetMapping("/post/{postId}/count")
	public ResponseEntity<Long> getCommentCount(@PathVariable Long postId) {
		logger.info("GET /api/v1/comments/post/{}/count", postId);
		return ResponseEntity.ok(commentService.getCommentCount(postId));
	}

	// =========================================================================
	// AUTHENTICATED — valid X-User-Id header required
	// =========================================================================

	@Operation(summary = "Add a comment to a post", security = @SecurityRequirement(name = "gateway-auth"))
	@PostMapping
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<CommentResponseDTO> addComment(@Valid @RequestBody CommentRequestDTO requestDTO,
			HttpServletRequest request) {

		Long userId = resolveUserId(request);
		// Override authorId from the trusted gateway header — never trust the request
		// body
		requestDTO.setAuthorId(userId);

		logger.info("POST /api/v1/comments — postId={} authorId={}", requestDTO.getPostId(), userId);
		CommentResponseDTO response = commentService.addComment(requestDTO);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@Operation(summary = "Update own comment", security = @SecurityRequirement(name = "gateway-auth"))
	@PutMapping("/{commentId}")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<CommentResponseDTO> updateComment(@PathVariable Long commentId,
			@Valid @RequestBody CommentUpdateDTO updateDTO, HttpServletRequest request) {

		Long userId = resolveUserId(request);
		logger.info("PUT /api/v1/comments/{} by userId={}", commentId, userId);
		CommentResponseDTO updated = commentService.updateComment(commentId, updateDTO, userId);
		return ResponseEntity.ok(updated);
	}

	@Operation(summary = "Soft-delete own comment (ADMIN/MOD can delete any)", security = @SecurityRequirement(name = "gateway-auth"))
	@DeleteMapping("/{commentId}")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Void> deleteComment(@PathVariable Long commentId, HttpServletRequest request) {

		Long userId = resolveUserId(request);
		String role = resolveUserRole(request);
		logger.info("DELETE /api/v1/comments/{} by userId={} role={}", commentId, userId, role);
		commentService.deleteComment(commentId, userId, role);
		return ResponseEntity.noContent().build();
	}

	@Operation(summary = "Get comments by user (auth required)")
	@GetMapping("/user/{authorId}")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<List<CommentResponseDTO>> getCommentsByUser(@PathVariable Long authorId) {
		logger.info("GET /api/v1/comments/user/{}", authorId);
		return ResponseEntity.ok(commentService.getCommentsByUser(authorId));
	}

	@Operation(summary = "Like a comment", security = @SecurityRequirement(name = "gateway-auth"))
	@PostMapping("/{commentId}/like")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Void> likeComment(@PathVariable Long commentId) {
		logger.info("POST /api/v1/comments/{}/like", commentId);
		commentService.likeComment(commentId);
		return ResponseEntity.ok().build();
	}

	@Operation(summary = "Unlike a comment", security = @SecurityRequirement(name = "gateway-auth"))
	@PostMapping("/{commentId}/unlike")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Void> unlikeComment(@PathVariable Long commentId) {
		logger.info("POST /api/v1/comments/{}/unlike", commentId);
		commentService.unlikeComment(commentId);
		return ResponseEntity.ok().build();
	}

	// =========================================================================
	// Helpers
	// =========================================================================

	// Reads the trusted userId injected by GatewayAuthFilter from request
	// attributes.
	private Long resolveUserId(HttpServletRequest request) {
		Object attr = request.getAttribute("X-User-Id");
		if (attr == null) {
			throw new UnauthorizedCommentAccessException("User identity header is missing.");
		}
		return (Long) attr;
	}

	private String resolveUserRole(HttpServletRequest request) {
		String role = request.getHeader("X-User-Role");
		return role != null ? role.toUpperCase(Locale.ROOT) : "USER";
	}
}
