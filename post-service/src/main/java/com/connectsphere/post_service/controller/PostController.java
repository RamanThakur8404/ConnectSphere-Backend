package com.connectsphere.post_service.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.connectsphere.post_service.constants.Visibility;
import com.connectsphere.post_service.dto.CreatePostRequest;
import com.connectsphere.post_service.dto.FeedRequest;
import com.connectsphere.post_service.dto.PostResponse;
import com.connectsphere.post_service.dto.SharePostRequest;
import com.connectsphere.post_service.dto.UpdatePostRequest;
import com.connectsphere.post_service.dto.ApiResponse;
import com.connectsphere.post_service.exception.PostNotFoundException;
import com.connectsphere.post_service.service.PostService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
@Tag(name = "Post API", description = "ConnectSphere post lifecycle — create, read, update, delete, feed, search, bookmark, share")
@SecurityRequirement(name = "BearerAuth")
public class PostController {

	private static final Logger log = LoggerFactory.getLogger(PostController.class);

	private final PostService postService;

	@Operation(summary = "Create a new post", description = "Creates a text or media post. Author is resolved from X-User-Id header.")
	@PostMapping
	public ResponseEntity<ApiResponse<PostResponse>> createPost(@Valid @RequestBody CreatePostRequest request,
			@RequestHeader(value = "X-User-Id", required = false) Integer headerUserId) {
		log.info("POST /api/v1/posts — authorId={}", request.getAuthorId());

		if (headerUserId != null) {
			request.setAuthorId(headerUserId);
		}
		log.info("Creating post for authorId={}", request.getAuthorId());
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Post created successfully", postService.createPost(request)));
	}

	@Operation(summary = "Get post by ID", description = "Returns a single post. Throws 404 if not found.")
	@GetMapping("/{postId}")
	public ResponseEntity<ApiResponse<PostResponse>> getPostById(@PathVariable @Positive int postId) {
		log.info("GET /api/v1/posts/{}", postId);

		PostResponse post = postService.getPostById(postId)
				.orElseThrow(() -> new PostNotFoundException("Post not found for postId: " + postId));

		return ResponseEntity.ok(ApiResponse.success("Post retrieved successfully", post));
	}

	@Operation(summary = "Get all posts by a user", description = "Returns all posts authored by the given userId.")
	@GetMapping("/user/{authorId}")
	public ResponseEntity<ApiResponse<List<PostResponse>>> getPostsByUser(@PathVariable @Positive int authorId) {
		log.info("GET /api/v1/posts/user/{}", authorId);

		return ResponseEntity.ok(ApiResponse.success("Posts retrieved successfully", postService.getPostsByUser(authorId)));
	}

	// PERSONALISED FEED
	@Operation(summary = "Get personalised news feed", description = "Returns all public posts plus followers-only posts from followed users, newest first. Supports cursor-based pagination.")
	@PostMapping("/feed")
	public ResponseEntity<ApiResponse<List<PostResponse>>> getPersonalisedFeed(@Valid @RequestBody FeedRequest feedRequest) {
		return ResponseEntity.ok(ApiResponse.success("Feed retrieved successfully", postService.getPersonalisedFeed(feedRequest)));
	}

	// PUBLIC FEED
	@Operation(summary = "Get public feed for guest users", description = "Returns public posts only. No auth required. Cursor-paginated.")
	@GetMapping("/public")
	public ResponseEntity<ApiResponse<List<PostResponse>>> getPublicFeed(@RequestParam(required = false) Integer cursor,
			@RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
		return ResponseEntity.ok(ApiResponse.success("Public feed retrieved successfully", postService.getPublicFeed(cursor, limit)));
	}

	// UPDATE
	@Operation(summary = "Update a post", description = "Updates content or visibility of an existing post. Only the author can update.")
	@PutMapping("/{postId}")
	public ResponseEntity<ApiResponse<PostResponse>> updatePost(@PathVariable @Positive int postId,
			@Valid @RequestBody UpdatePostRequest request) {
		return ResponseEntity.ok(ApiResponse.success("Post updated successfully", postService.updatePost(postId, request)));
	}

	// DELETE
	@Operation(summary = "Soft-delete a post", description = "Marks post as deleted. Media is retained for 30 days before purge.")
	@DeleteMapping("/{postId}")
	public ResponseEntity<ApiResponse<Map<String, String>>> deletePost(@PathVariable @Positive int postId) {
		postService.deletePost(postId);
		return ResponseEntity.ok(ApiResponse.success("Post deleted successfully", Map.of("message", "Post deleted successfully. postId: " + postId)));
	}

	// SEARCH
	@Operation(summary = "Search posts by keyword", description = "Full-text search in post content. Returns PUBLIC posts only.")
	@GetMapping("/search")
	public ResponseEntity<ApiResponse<List<PostResponse>>> searchPosts(@RequestParam String keyword) {
		return ResponseEntity.ok(ApiResponse.success("Search results retrieved", postService.searchPosts(keyword)));
	}

	// VISIBILITY
	@Operation(summary = "Change post visibility", description = "Updates visibility to PUBLIC, PRIVATE, or FOLLOWERS_ONLY.")
	@PutMapping("/{postId}/visibility")
	public ResponseEntity<ApiResponse<Map<String, String>>> changeVisibility(@PathVariable @Positive int postId,
			@RequestParam Visibility visibility) {
		postService.changeVisibility(postId, visibility);
		return ResponseEntity.ok(ApiResponse.success("Visibility updated", Map.of("message", "Visibility updated to '" + visibility + "' for postId: " + postId)));
	}

	// ENGAGEMENT COUNTERS (called by like-service / comment-service)
	@Operation(summary = "Increment like count", description = "Internal endpoint called by like-service. Not for direct client use.")
	@PostMapping("/{postId}/like")
	public ResponseEntity<ApiResponse<Map<String, String>>> incrementLikes(@PathVariable @Positive int postId) {
		postService.incrementLikes(postId);
		return ResponseEntity.ok(ApiResponse.success("Like count incremented", Map.of("message", "Like count incremented for postId: " + postId)));
	}

	@Operation(summary = "Decrement like count", description = "Internal endpoint called by like-service when a reaction is removed.")
	@PostMapping("/{postId}/unlike")
	public ResponseEntity<ApiResponse<Map<String, String>>> decrementLikes(@PathVariable @Positive int postId) {
		postService.decrementLikes(postId);
		return ResponseEntity.ok(ApiResponse.success("Like count decremented", Map.of("message", "Like count decremented for postId: " + postId)));
	}

	@Operation(summary = "Increment comment count", description = "Internal endpoint called by comment-service on new comment.")
	@PostMapping("/{postId}/comment")
	public ResponseEntity<ApiResponse<Map<String, String>>> incrementComments(@PathVariable @Positive int postId) {
		postService.incrementComments(postId);
		return ResponseEntity.ok(ApiResponse.success("Comment count incremented", Map.of("message", "Comment count incremented for postId: " + postId)));
	}

	@Operation(summary = "Decrement comment count", description = "Internal endpoint called by comment-service on comment delete.")
	@PostMapping("/{postId}/uncomment")
	public ResponseEntity<ApiResponse<Map<String, String>>> decrementComments(@PathVariable @Positive int postId) {
		postService.decrementComments(postId);
		return ResponseEntity.ok(ApiResponse.success("Comment count decremented", Map.of("message", "Comment count decremented for postId: " + postId)));
	}

	@Operation(summary = "Increment share count", description = "Increments sharesCount on the original post when it is reshared.")
	@PostMapping("/{postId}/share-count")
	public ResponseEntity<ApiResponse<Map<String, String>>> incrementShares(@PathVariable @Positive int postId) {
		postService.incrementShares(postId);
		return ResponseEntity.ok(ApiResponse.success("Share count incremented", Map.of("message", "Share count incremented for postId: " + postId)));
	}

	// SHARE
	@Operation(summary = "Share / repost a post", description = "Creates a new post referencing the original. Increments sharesCount on original.")
	@PostMapping("/{postId}/share")
	public ResponseEntity<ApiResponse<PostResponse>> sharePost(@PathVariable @Positive int postId,
			@Valid @RequestBody SharePostRequest request,
			@RequestHeader(value = "X-User-Id", required = false) Integer headerUserId) {

		if (headerUserId != null) {
			request.setAuthorId(headerUserId);
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Post shared successfully", postService.sharePost(postId, request)));
	}

	@Operation(summary = "Get all reshares of a post", description = "Returns all posts that reference this post as their original.")
	@GetMapping("/{postId}/shares")
	public ResponseEntity<ApiResponse<List<PostResponse>>> getSharesOfPost(@PathVariable @Positive int postId) {
		return ResponseEntity.ok(ApiResponse.success("Shares retrieved", postService.getSharesOfPost(postId)));
	}

	// BOOKMARK

	@Operation(summary = "Bookmark a post", description = "Saves the post to the user's private bookmark list. Idempotent.")
	@PostMapping("/{postId}/bookmark")
	public ResponseEntity<ApiResponse<Map<String, String>>> bookmarkPost(@PathVariable @Positive int postId,
			@RequestHeader("X-User-Id") Integer headerUserId) {

		postService.bookmarkPost(headerUserId, postId);
		return ResponseEntity.ok(ApiResponse.success("Post bookmarked", Map.of("message", "Post bookmarked. postId: " + postId)));
	}

	@Operation(summary = "Remove a bookmark", description = "Removes the post from the user's bookmark list.")
	@DeleteMapping("/{postId}/bookmark")
	public ResponseEntity<ApiResponse<Map<String, String>>> removeBookmark(@PathVariable @Positive int postId,
			@RequestHeader("X-User-Id") Integer headerUserId) {

		postService.removeBookmark(headerUserId, postId);
		return ResponseEntity.ok(ApiResponse.success("Bookmark removed", Map.of("message", "Bookmark removed. postId: " + postId)));
	}

	@Operation(summary = "Get all bookmarked posts for a user", description = "Returns all posts saved by the given userId.")
	@GetMapping("/bookmarks")
	public ResponseEntity<ApiResponse<List<PostResponse>>> getMyBookmarkedPosts(
			@RequestHeader("X-User-Id") Integer headerUserId) {
		return ResponseEntity.ok(ApiResponse.success("Bookmarks retrieved", postService.getBookmarkedPosts(headerUserId)));
	}

	@Operation(summary = "Get all bookmarked posts for a user", description = "Returns all posts saved by the given userId.")
	@GetMapping("/bookmarks/{userId}")
	public ResponseEntity<ApiResponse<List<PostResponse>>> getBookmarkedPosts(@PathVariable @Positive int userId) {
		return ResponseEntity.ok(ApiResponse.success("Bookmarks retrieved", postService.getBookmarkedPosts(userId)));
	}

	// POST COUNT

	@Operation(summary = "Get post count for a user", description = "Returns total number of posts authored by the given userId.")
	@GetMapping("/count/{authorId}")
	public ResponseEntity<ApiResponse<Map<String, Integer>>> getPostCount(@PathVariable @Positive int authorId) {
		return ResponseEntity.ok(ApiResponse.success("Post count retrieved", Map.of("postCount", postService.getPostCount(authorId))));
	}
}
