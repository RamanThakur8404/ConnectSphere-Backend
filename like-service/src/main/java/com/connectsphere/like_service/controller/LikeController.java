package com.connectsphere.like_service.controller;

import java.util.List;
import java.util.Map;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.connectsphere.like_service.constant.ReactionType;
import com.connectsphere.like_service.constant.TargetType;
import com.connectsphere.like_service.dto.ApiResponse;
import com.connectsphere.like_service.dto.ChangeReactionDTO;
import com.connectsphere.like_service.dto.LikeRequestDTO;
import com.connectsphere.like_service.dto.LikeResponseDTO;
import com.connectsphere.like_service.dto.ReactionSummaryDTO;
import com.connectsphere.like_service.service.LikeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/likes")
@RequiredArgsConstructor
public class LikeController {

	private static final Logger log = LoggerFactory.getLogger(LikeController.class);

	private final LikeService likeService;

	@PostMapping
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ApiResponse<LikeResponseDTO>> likeTarget(@Valid @RequestBody LikeRequestDTO request) {

		log.info("POST /api/v1/likes – user {} reacting to {} {}", request.getUserId(), request.getTargetType(),
				request.getTargetId());

		LikeResponseDTO response = likeService.likeTarget(request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success("Reaction added successfully", response));
	}

	@DeleteMapping
	public ResponseEntity<ApiResponse<Void>> unlikeTarget(@RequestParam Integer userId, @RequestParam Integer targetId,
			@RequestParam TargetType targetType) {

		log.info("DELETE /api/v1/likes – user {} removing reaction from {} {}", userId, targetType, targetId);

		likeService.unlikeTarget(userId, targetId, targetType);
		return ResponseEntity.ok(ApiResponse.success("Reaction removed successfully", null));
	}

	@GetMapping("/has-liked")
	public ResponseEntity<ApiResponse<Boolean>> hasLiked(@RequestParam Integer userId, @RequestParam Integer targetId,
			@RequestParam TargetType targetType) {

		log.debug("GET /api/v1/likes/has-liked – user {} on {} {}", userId, targetType, targetId);

		boolean result = likeService.hasLiked(userId, targetId, targetType);
		return ResponseEntity.ok(ApiResponse.success("Has liked status retrieved", result));
	}

	@PostMapping("/has-liked-batch")
	public ResponseEntity<ApiResponse<Map<Integer, Boolean>>> hasLikedBatch(@RequestParam Integer userId,
			@RequestParam TargetType targetType, @RequestBody List<Integer> targetIds) {

		log.debug("POST /api/v1/likes/has-liked-batch – user {} on {} targets", userId, targetType);

		Map<Integer, Boolean> result = likeService.hasLikedBatch(userId, targetIds, targetType);
		return ResponseEntity.ok(ApiResponse.success("Has liked batch status retrieved", result));
	}

	@GetMapping("/target/{targetId}")
	public ResponseEntity<ApiResponse<List<LikeResponseDTO>>> getLikesByTarget(@PathVariable Integer targetId,
			@RequestParam TargetType targetType) {

		log.debug("GET /api/v1/likes/target/{} – type {}", targetId, targetType);

		List<LikeResponseDTO> likes = likeService.getLikesByTarget(targetId, targetType);
		return ResponseEntity.ok(ApiResponse.success("Reactions retrieved", likes));
	}

	@GetMapping("/user/{userId}")
	public ResponseEntity<ApiResponse<List<LikeResponseDTO>>> getLikesByUser(@PathVariable Integer userId) {

		log.debug("GET /api/v1/likes/user/{}", userId);

		List<LikeResponseDTO> likes = likeService.getLikesByUser(userId);
		return ResponseEntity.ok(ApiResponse.success("User reactions retrieved", likes));
	}

	@GetMapping("/count")
	public ResponseEntity<ApiResponse<Integer>> getLikeCount(@RequestParam Integer targetId,
			@RequestParam TargetType targetType) {

		log.debug("GET /api/v1/likes/count – {} {}", targetType, targetId);

		int count = likeService.getLikeCount(targetId, targetType);
		return ResponseEntity.ok(ApiResponse.success("Reaction count retrieved", count));
	}

	@GetMapping("/count/by-type")
	public ResponseEntity<ApiResponse<Integer>> getLikeCountByType(@RequestParam Integer targetId,
			@RequestParam TargetType targetType, @RequestParam ReactionType reactionType) {

		log.debug("GET /api/v1/likes/count/by-type – {} {} {}", reactionType, targetType, targetId);

		int count = likeService.getLikeCountByType(targetId, targetType, reactionType);
		return ResponseEntity.ok(ApiResponse.success("Reaction count by type retrieved", count));
	}

	@GetMapping("/summary/{targetId}")
	public ResponseEntity<ApiResponse<ReactionSummaryDTO>> getReactionSummary(@PathVariable Integer targetId,
			@RequestParam TargetType targetType) {

		log.debug("GET /api/v1/likes/summary/{} – type {}", targetId, targetType);

		ReactionSummaryDTO summary = likeService.getReactionSummary(targetId, targetType);
		return ResponseEntity.ok(ApiResponse.success("Reaction summary retrieved", summary));
	}

	@PutMapping("/change-reaction")
	public ResponseEntity<ApiResponse<LikeResponseDTO>> changeReaction(@Valid @RequestBody ChangeReactionDTO request) {

		log.info("PUT /api/v1/likes/change-reaction – user {} changing reaction on {} {}", request.getUserId(),
				request.getTargetType(), request.getTargetId());

		LikeResponseDTO response = likeService.changeReaction(request);
		return ResponseEntity.ok(ApiResponse.success("Reaction changed successfully", response));
	}
}
