package com.connectsphere.post_service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.connectsphere.post_service.client.SearchServiceClient;
import com.connectsphere.post_service.config.RabbitMQConfig;
import com.connectsphere.post_service.constants.PostType;
import com.connectsphere.post_service.constants.Visibility;
import com.connectsphere.post_service.dto.CreatePostRequest;
import com.connectsphere.post_service.dto.FeedRequest;
import com.connectsphere.post_service.dto.PostEventMessage;
import com.connectsphere.post_service.dto.PostResponse;
import com.connectsphere.post_service.dto.SharePostRequest;
import com.connectsphere.post_service.dto.UpdatePostRequest;
import com.connectsphere.post_service.entity.Bookmark;
import com.connectsphere.post_service.entity.Post;
import com.connectsphere.post_service.exception.DuplicateBookmarkException;
import com.connectsphere.post_service.exception.PostNotFoundException;
import com.connectsphere.post_service.exception.UnauthorizedPostActionException;
import com.connectsphere.post_service.mapper.PostMapper;
import com.connectsphere.post_service.repository.BookmarkRepository;
import com.connectsphere.post_service.repository.PostRepository;

import lombok.RequiredArgsConstructor;

// PostServiceImpl — business logic for the ConnectSphere post-service.

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

	private static final Logger log = LoggerFactory.getLogger(PostServiceImpl.class);
	private static final int MAX_PUBLIC_FEED_LIMIT = 50;

	private final PostRepository postRepository;
	private final BookmarkRepository bookmarkRepository;
	private final PostMapper postMapper;
	private final RabbitTemplate rabbitTemplate;
	private final SearchServiceClient searchServiceClient;
	private final HashtagExtractor hashtagExtractor;

	// =========================================================================
	// CREATE
	// =========================================================================

	@Override
	@Transactional
	public PostResponse createPost(CreatePostRequest request) {
		log.info("createPost — authorId={}, visibility={}", request.getAuthorId(), request.getVisibility());

		// Auto-resolve postType if not provided
		if (request.getPostType() == null) {
			PostType resolved = (request.getMediaUrls() != null && !request.getMediaUrls().isEmpty()) ? PostType.MEDIA
					: PostType.TEXT;
			request.setPostType(resolved);
			log.debug("postType auto-resolved to {}", resolved);
		}

		Post post = postMapper.toEntity(request);
		post.setHashtags(resolveHashtags(post));
		Post saved = postRepository.save(post);
		log.info("Post created — postId={}", saved.getPostId());

		// Publish RabbitMQ event (notification-service + search-service consume this)
		publishPostEvent(saved, PostEventMessage.EventType.POST_CREATED);

		// Synchronous search index call (best-effort — failure won't rollback post
		// creation)
		indexPostInSearch(saved);

		return postMapper.toResponse(saved);
	}

	// =========================================================================
	// READ
	// =========================================================================

	@Override
	@Transactional(readOnly = true)
	public Optional<PostResponse> getPostById(int postId) {
		log.info("getPostById — postId={}", postId);
		return postRepository.findByPostIdAndIsDeletedFalse(postId).map(postMapper::toResponse);
	}

	@Override
	@Transactional(readOnly = true)
	public List<PostResponse> getPostsByUser(int authorId) {
		log.info("getPostsByUser — authorId={}", authorId);
		List<Post> posts = postRepository.findByAuthorIdAndIsDeletedFalseOrderByCreatedAtDesc(authorId);
		log.debug("getPostsByUser — {} posts found for authorId={}", posts.size(), authorId);
		return postMapper.toResponseList(posts);
	}

	@Override
	@Transactional(readOnly = true)
	public List<PostResponse> getPersonalisedFeed(FeedRequest feedRequest) {
		int followeeCount = feedRequest.getFolloweeIds() != null ? feedRequest.getFolloweeIds().size() : 0;
		log.info("getPersonalisedFeed — followeeCount={}, cursor={}, limit={}", followeeCount,
				feedRequest.getCursor(), feedRequest.getLimit());

		if (feedRequest.getFolloweeIds() == null || feedRequest.getFolloweeIds().isEmpty()) {
			log.warn("getPersonalisedFeed — empty followeeIds, returning empty feed");
			return List.of();
		}

		int limit = Math.max(1, Math.min(feedRequest.getLimit(), MAX_PUBLIC_FEED_LIMIT));
		List<Post> feed = postRepository.findFeedByUserIdsWithCursor(feedRequest.getFolloweeIds(), LocalDateTime.now(),
				feedRequest.getCursor(), PageRequest.of(0, limit));

		log.debug("getPersonalisedFeed — {} posts returned", feed.size());
		return postMapper.toResponseList(feed);
	}

	@Override
	@Transactional(readOnly = true)
	public List<PostResponse> getPublicFeed(Integer cursor, int limit) {
		log.info("getPublicFeed — cursor={}, limit={}", cursor, limit);
		int safeLimit = Math.max(1, Math.min(limit, MAX_PUBLIC_FEED_LIMIT));
		List<Post> posts = postRepository.findPublicPostsWithCursor(LocalDateTime.now(), cursor,
				PageRequest.of(0, safeLimit));
		return postMapper.toResponseList(posts);
	}

	// =========================================================================
	// UPDATE
	// =========================================================================

	@Override
	@Transactional
	public PostResponse updatePost(int postId, UpdatePostRequest request) {
		log.info("updatePost — postId={}", postId);

		Post existing = fetchActivePostOrThrow(postId);
		postMapper.applyUpdate(request, existing);
		existing.setHashtags(resolveHashtags(existing));
		Post saved = postRepository.save(existing);

		log.info("Post updated — postId={}", postId);

		// Publish update event so search-service can re-index
		publishPostEvent(saved, PostEventMessage.EventType.POST_UPDATED);

		// Keep local development reliable even when RabbitMQ is not running.
		indexPostInSearch(saved);

		return postMapper.toResponse(saved);
	}

	// =========================================================================
	// DELETE (soft-delete)
	// =========================================================================

	@Override
	@Transactional
	public void deletePost(int postId) {
		log.info("deletePost — postId={}", postId);

		Post post = fetchActivePostOrThrow(postId);
		post.setDeleted(true);
		postRepository.save(post);

		log.info("Post soft-deleted — postId={}", postId);

		// Publish delete event so search-service removes the index entry
		publishPostEvent(post, PostEventMessage.EventType.POST_DELETED);

		// Remove from search index synchronously (best-effort)
		removePostFromSearch(postId);
	}

	// =========================================================================
	// SEARCH
	// =========================================================================

	@Override
	@Transactional(readOnly = true)
	public List<PostResponse> searchPosts(String keyword) {
		log.info("searchPosts — keyword='{}'", keyword);

		if (keyword == null || keyword.isBlank()) {
			log.warn("searchPosts — blank keyword, returning empty list");
			return List.of();
		}

		List<Post> results = postRepository.searchByContent(keyword.trim());
		log.debug("searchPosts — {} results for keyword='{}'", results.size(), keyword);
		return postMapper.toResponseList(results);
	}

	// =========================================================================
	// ENGAGEMENT COUNTERS
	// =========================================================================

	@Override
	@Transactional
	public void incrementLikes(int postId) {
		log.info("incrementLikes — postId={}", postId);
		int updated = postRepository.updateLikesCount(postId, 1);
		if (updated == 0)
			throw new PostNotFoundException("Post not found — postId: " + postId);
	}

	@Override
	@Transactional
	public void decrementLikes(int postId) {
		log.info("decrementLikes — postId={}", postId);
		int updated = postRepository.updateLikesCount(postId, -1);
		if (updated == 0)
			throw new PostNotFoundException("Post not found — postId: " + postId);
	}

	@Override
	@Transactional
	public void incrementComments(int postId) {
		log.info("incrementComments — postId={}", postId);
		int updated = postRepository.updateCommentsCount(postId, 1);
		if (updated == 0)
			throw new PostNotFoundException("Post not found — postId: " + postId);
	}

	@Override
	@Transactional
	public void decrementComments(int postId) {
		log.info("decrementComments — postId={}", postId);
		int updated = postRepository.updateCommentsCount(postId, -1);
		if (updated == 0)
			throw new PostNotFoundException("Post not found — postId: " + postId);
	}

	@Override
	@Transactional
	public void incrementShares(int postId) {
		log.info("incrementShares — postId={}", postId);
		int updated = postRepository.incrementSharesCount(postId);
		if (updated == 0)
			throw new PostNotFoundException("Post not found — postId: " + postId);
	}

	// =========================================================================
	// VISIBILITY
	// =========================================================================

	@Override
	@Transactional
	public void changeVisibility(int postId, Visibility visibility) {
		log.info("changeVisibility — postId={}, newVisibility={}", postId, visibility);
		Post post = fetchActivePostOrThrow(postId);
		post.setVisibility(visibility);
		postRepository.save(post);
		log.info("Visibility changed to {} for postId={}", visibility, postId);
	}

	// =========================================================================
	// STATS
	// =========================================================================

	@Override
	@Transactional(readOnly = true)
	public int getPostCount(int authorId) {
		log.info("getPostCount — authorId={}", authorId);
		long count = postRepository.countByAuthorIdAndIsDeletedFalse(authorId);
		log.debug("postCount={} for authorId={}", count, authorId);
		return (int) count;
	}

	// =========================================================================
	// SHARE / REPOST (NEW)
	// =========================================================================

	@Override
	@Transactional
	public PostResponse sharePost(int originalPostId, SharePostRequest request) {
		log.info("sharePost — originalPostId={}, sharingUserId={}", originalPostId, request.getAuthorId());

		// Original post must exist
		Post original = fetchActivePostOrThrow(originalPostId);

		// Build the share post — content is the share comment (or original content)
		String shareContent = (request.getShareComment() != null && !request.getShareComment().isBlank())
				? request.getShareComment()
				: "Shared post";

		Post sharePost = Post.builder().authorId(request.getAuthorId()).content(shareContent).mediaUrls(List.of())
				.postType(PostType.TEXT)
				.visibility(request.getVisibility() != null ? request.getVisibility() : Visibility.PUBLIC)
				.originalPostId(originalPostId).contentWarning(original.isContentWarning()).build();
		sharePost.setHashtags(resolveHashtags(sharePost));

		Post saved = postRepository.save(sharePost);

		// Increment share counter on original post
		postRepository.incrementSharesCount(originalPostId);

		log.info("Share post created — sharePostId={}, originalPostId={}", saved.getPostId(), originalPostId);

		publishPostEvent(saved, PostEventMessage.EventType.POST_CREATED);

		return postMapper.toResponse(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public List<PostResponse> getSharesOfPost(int originalPostId) {
		log.info("getSharesOfPost — originalPostId={}", originalPostId);
		// Validate original exists
		fetchActivePostOrThrow(originalPostId);
		List<Post> shares = postRepository.findByOriginalPostIdAndIsDeletedFalse(originalPostId);
		return postMapper.toResponseList(shares);
	}

	// =========================================================================
	// BOOKMARK (NEW)
	// =========================================================================

	@Override
	@Transactional
	public void bookmarkPost(int userId, int postId) {
		log.info("bookmarkPost — userId={}, postId={}", userId, postId);

		// Validate post exists
		fetchActivePostOrThrow(postId);

		if (bookmarkRepository.existsByUserIdAndPostId(userId, postId)) {
			throw new DuplicateBookmarkException("Post already bookmarked. userId=" + userId + ", postId=" + postId);
		}

		Bookmark bookmark = Bookmark.builder().userId(userId).postId(postId).build();
		bookmarkRepository.save(bookmark);
		log.info("Bookmark saved — userId={}, postId={}", userId, postId);
	}

	@Override
	@Transactional
	public void removeBookmark(int userId, int postId) {
		log.info("removeBookmark — userId={}, postId={}", userId, postId);

		if (!bookmarkRepository.existsByUserIdAndPostId(userId, postId)) {
			throw new PostNotFoundException("Bookmark not found for userId=" + userId + ", postId=" + postId);
		}

		bookmarkRepository.deleteByUserIdAndPostId(userId, postId);
		log.info("Bookmark removed — userId={}, postId={}", userId, postId);
	}

	@Override
	@Transactional(readOnly = true)
	public List<PostResponse> getBookmarkedPosts(int userId) {
		log.info("getBookmarkedPosts — userId={}", userId);

		List<Integer> postIds = bookmarkRepository.findPostIdsByUserId(userId);
		if (postIds.isEmpty())
			return List.of();

		// Fetch posts in bulk and preserve bookmark order
		List<Post> posts = postIds.stream().map(pid -> postRepository.findByPostIdAndIsDeletedFalse(pid))
				.flatMap(Optional::stream).toList();

		log.debug("getBookmarkedPosts — {} posts found for userId={}", posts.size(), userId);
		return postMapper.toResponseList(posts);
	}

	// =========================================================================
	// Private Helpers
	// =========================================================================

	private Post fetchActivePostOrThrow(int postId) {
		return postRepository.findByPostIdAndIsDeletedFalse(postId).orElseThrow(() -> {
			log.error("PostNotFoundException — postId={} not found or deleted", postId);
			return new PostNotFoundException("Post not found or has been deleted. PostId: " + postId);
		});
	}

	private void publishPostEvent(Post post, PostEventMessage.EventType eventType) {
		try {
			String routingKey = switch (eventType) {
			case POST_CREATED -> RabbitMQConfig.POST_CREATED_ROUTING_KEY;
			case POST_DELETED -> RabbitMQConfig.POST_DELETED_ROUTING_KEY;
			case POST_UPDATED -> RabbitMQConfig.POST_UPDATED_ROUTING_KEY;
			};

			PostEventMessage message = PostEventMessage.builder().eventType(eventType).postId(post.getPostId())
					.authorId(post.getAuthorId()).content(post.getContent()).mediaUrls(post.getMediaUrls())
					.hashtags(post.getHashtags())
					.visibility(post.getVisibility() != null ? post.getVisibility().name() : "PUBLIC")
					.postType(post.getPostType() != null ? post.getPostType().name() : "TEXT")
					.contentWarning(post.isContentWarning()).createdAt(post.getCreatedAt()).build();

			rabbitTemplate.convertAndSend(RabbitMQConfig.POST_EXCHANGE, routingKey, message);
			log.info("RabbitMQ event published — eventType={}, postId={}", eventType, post.getPostId());
		} catch (Exception ex) {
			// Non-blocking: log the error but don't fail the main transaction
			log.error("Failed to publish RabbitMQ event — eventType={}, postId={}, error={}", eventType,
					post.getPostId(), ex.getMessage());
		}
	}

	private void indexPostInSearch(Post post) {
		try {
			java.util.Map<String, Object> indexRequest = new java.util.HashMap<>();
			indexRequest.put("postId", post.getPostId());
			indexRequest.put("authorId", post.getAuthorId());
			indexRequest.put("content", post.getContent());
			indexRequest.put("hashtags", post.getHashtags() != null ? post.getHashtags() : List.of());
			indexRequest.put("visibility", post.getVisibility() != null ? post.getVisibility().name() : "PUBLIC");
			indexRequest.put("createdAt",
					post.getCreatedAt() != null ? post.getCreatedAt().toString() : LocalDateTime.now().toString());
			searchServiceClient.indexPost(String.valueOf(post.getAuthorId()), "USER", indexRequest);
			log.debug("Post indexed in search — postId={}", post.getPostId());
		} catch (Exception ex) {
			log.warn("Search indexing failed (non-critical) — postId={}, error={}", post.getPostId(), ex.getMessage());
		}
	}

	private void removePostFromSearch(int postId) {
		try {
			searchServiceClient.removePostIndex("0", "ADMIN", postId);
			log.debug("Post removed from search index — postId={}", postId);
		} catch (Exception ex) {
			log.warn("Search de-indexing failed (non-critical) — postId={}, error={}", postId, ex.getMessage());
		}
	}

	private List<String> resolveHashtags(Post post) {
		return hashtagExtractor.resolveTags(post.getContent(), post.getMediaUrls());
	}
}
