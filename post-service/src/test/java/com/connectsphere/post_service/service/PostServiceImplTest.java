package com.connectsphere.post_service.service;

import com.connectsphere.post_service.client.SearchServiceClient;
import com.connectsphere.post_service.constants.PostType;
import com.connectsphere.post_service.constants.Visibility;
import com.connectsphere.post_service.dto.CreatePostRequest;
import com.connectsphere.post_service.dto.FeedRequest;
import com.connectsphere.post_service.dto.PostResponse;
import com.connectsphere.post_service.dto.UpdatePostRequest;
import com.connectsphere.post_service.entity.Post;
import com.connectsphere.post_service.exception.PostNotFoundException;
import com.connectsphere.post_service.mapper.PostMapper;
import com.connectsphere.post_service.repository.BookmarkRepository;
import com.connectsphere.post_service.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

	@Mock
	private PostRepository postRepository;

	@Mock
	private BookmarkRepository bookmarkRepository;

	@Mock
	private RabbitTemplate rabbitTemplate;

	@Mock
	private SearchServiceClient searchServiceClient;

	@Spy
	private PostMapper postMapper = new PostMapper();

	@Spy
	private HashtagExtractor hashtagExtractor = new HashtagExtractor();

	@InjectMocks
	private PostServiceImpl postService;

	private Post samplePost;

	@BeforeEach
	void setUp() {
		samplePost = new Post();
		samplePost.setPostId(1);
		samplePost.setAuthorId(100);
		samplePost.setContent("Hello ConnectSphere! #java");
		samplePost.setHashtags(List.of("java", "connectsphere"));
		samplePost.setPostType(PostType.TEXT);
		samplePost.setVisibility(Visibility.PUBLIC);
		samplePost.setLikesCount(0);
		samplePost.setCommentsCount(0);
		samplePost.setDeleted(false);
		samplePost.setCreatedAt(LocalDateTime.now());
		samplePost.setUpdatedAt(LocalDateTime.now());
	}

	@Test
	@DisplayName("createPost — valid post should be saved and returned")
	void testCreatePost_Success() {
		CreatePostRequest req = new CreatePostRequest();
		req.setAuthorId(100);
		req.setContent("Hello ConnectSphere! #java");
		req.setVisibility(Visibility.PUBLIC);

		when(postRepository.save(any(Post.class))).thenReturn(samplePost);

		PostResponse result = postService.createPost(req);

		assertNotNull(result);
		assertEquals(1, result.getPostId());
		assertEquals(Visibility.PUBLIC, result.getVisibility());
		assertTrue(result.getHashtags().contains("java"));
		verify(postRepository, times(1)).save(any(Post.class));
		verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(Object.class));
	}

	@Test
	@DisplayName("createPost â€” generates useful hashtags when content has no explicit tags")
	void testCreatePost_GeneratesBackendHashtags() {
		CreatePostRequest req = new CreatePostRequest();
		req.setAuthorId(100);
		req.setContent("React UI design for the dashboard");
		req.setMediaUrls(Collections.emptyList());

		when(postRepository.save(any(Post.class))).thenAnswer(inv -> {
			Post p = inv.getArgument(0);
			p.setPostId(4);
			p.setCreatedAt(LocalDateTime.now());
			p.setUpdatedAt(LocalDateTime.now());
			return p;
		});

		PostResponse result = postService.createPost(req);

		assertTrue(result.getHashtags().contains("frontend"));
		verify(searchServiceClient).indexPost(anyString(), anyString(), argThat(payload ->
				payload.containsKey("hashtags") && ((List<?>) payload.get("hashtags")).contains("frontend")));
	}

	@Test
	@DisplayName("createPost — null postType auto-resolved to TEXT when no media")
	void testCreatePost_AutoSetPostType_Text() {
		CreatePostRequest req = new CreatePostRequest();
		req.setAuthorId(100);
		req.setContent("Hello text");
		req.setMediaUrls(Collections.emptyList());

		when(postRepository.save(any(Post.class))).thenAnswer(inv -> {
			Post p = inv.getArgument(0);
			p.setPostId(2);
			return p;
		});

		PostResponse result = postService.createPost(req);

		assertEquals(PostType.TEXT, result.getPostType());
	}

	@Test
	@DisplayName("createPost — null postType auto-resolved to MEDIA when mediaUrls present")
	void testCreatePost_AutoSetPostType_Media() {
		CreatePostRequest req = new CreatePostRequest();
		req.setAuthorId(100);
		req.setContent("Hello media");
		req.setMediaUrls(List.of("https://cdn.connectsphere.com/img1.jpg"));

		when(postRepository.save(any(Post.class))).thenAnswer(inv -> {
			Post p = inv.getArgument(0);
			p.setPostId(3);
			return p;
		});

		PostResponse result = postService.createPost(req);

		assertEquals(PostType.MEDIA, result.getPostType());
	}

	@Test
	@DisplayName("getPostById — existing post returns non-empty Optional")
	void testGetPostById_Found() {
		when(postRepository.findByPostIdAndIsDeletedFalse(1)).thenReturn(Optional.of(samplePost));

		Optional<PostResponse> result = postService.getPostById(1);

		assertTrue(result.isPresent());
		assertEquals(1, result.get().getPostId());
	}

	@Test
	@DisplayName("getPostById — non-existent postId returns empty Optional")
	void testGetPostById_NotFound() {
		when(postRepository.findByPostIdAndIsDeletedFalse(99)).thenReturn(Optional.empty());
		Optional<PostResponse> result = postService.getPostById(99);
		assertTrue(result.isEmpty());
	}

	@Test
	@DisplayName("getPostsByUser — returns list of posts for given author")
	void testGetPostsByUser_ReturnsList() {
		when(postRepository.findByAuthorIdAndIsDeletedFalseOrderByCreatedAtDesc(100))
				.thenReturn(List.of(samplePost));

		List<PostResponse> posts = postService.getPostsByUser(100);

		assertFalse(posts.isEmpty());
		assertEquals(1, posts.size());
		assertEquals(100, posts.get(0).getAuthorId());
	}

	@Test
	@DisplayName("getPersonalisedFeed — valid followeeIds returns public and followed-user feed posts")
	void testGetFeedForUser_ReturnsFeed() {
		when(postRepository.findFeedByUserIdsWithCursor(eq(List.of(101, 102)), any(), any(), any()))
				.thenReturn(List.of(samplePost));

		FeedRequest req = new FeedRequest();
		req.setFolloweeIds(List.of(101, 102));
		req.setLimit(20);

		List<PostResponse> feed = postService.getPersonalisedFeed(req);

		assertEquals(1, feed.size());
		verify(postRepository, times(1)).findFeedByUserIdsWithCursor(any(), any(), any(), any());
	}

	@Test
	@DisplayName("getPersonalisedFeed — empty followeeIds returns empty list without DB call")
	void testGetFeedForUser_EmptyIds_NoDbCall() {
		FeedRequest req = new FeedRequest();
		req.setFolloweeIds(Collections.emptyList());
		List<PostResponse> feed = postService.getPersonalisedFeed(req);
		assertTrue(feed.isEmpty());
		verify(postRepository, never()).findFeedByUserIdsWithCursor(any(), any(), any(), any());
	}

	@Test
	@DisplayName("getPersonalisedFeed — null followeeIds returns empty list without DB call")
	void testGetFeedForUser_NullIds_NoDbCall() {
		FeedRequest req = new FeedRequest();
		req.setFolloweeIds(null);
		List<PostResponse> feed = postService.getPersonalisedFeed(req);

		assertTrue(feed.isEmpty());
		verify(postRepository, never()).findFeedByUserIdsWithCursor(any(), any(), any(), any());
	}

	@Test
	@DisplayName("updatePost — should update content and return updated post")
	void testUpdatePost_Success() {
		UpdatePostRequest req = new UpdatePostRequest();
		req.setContent("Updated content");
		req.setPostType(PostType.TEXT);

		when(postRepository.findByPostIdAndIsDeletedFalse(1)).thenReturn(Optional.of(samplePost));
		when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

		PostResponse result = postService.updatePost(1, req);

		assertEquals("Updated content", result.getContent());
		verify(postRepository, times(1)).save(samplePost);
	}

	@Test
	@DisplayName("updatePost — non-existent postId throws PostNotFoundException")
	void testUpdatePost_PostNotFound() {
		when(postRepository.findByPostIdAndIsDeletedFalse(999)).thenReturn(Optional.empty());

		UpdatePostRequest req = new UpdatePostRequest();
		req.setContent("New content");

		assertThrows(PostNotFoundException.class, () -> postService.updatePost(999, req));
	}

	@Test
	@DisplayName("deletePost — should soft-delete by setting isDeleted=true")
	void testDeletePost_SetsIsDeletedTrue() {
		when(postRepository.findByPostIdAndIsDeletedFalse(1)).thenReturn(Optional.of(samplePost));
		when(postRepository.save(any(Post.class))).thenReturn(samplePost);

		postService.deletePost(1);

		assertTrue(samplePost.isDeleted());
		verify(postRepository, times(1)).save(samplePost);
	}

	@Test
	@DisplayName("deletePost — non-existent postId throws PostNotFoundException")
	void testDeletePost_PostNotFound() {
		when(postRepository.findByPostIdAndIsDeletedFalse(500)).thenReturn(Optional.empty());
		assertThrows(PostNotFoundException.class, () -> postService.deletePost(500));
	}

	@Test
	@DisplayName("searchPosts — keyword match returns results")
	void testSearchPosts_Found() {
		when(postRepository.searchByContent("java")).thenReturn(List.of(samplePost));

		List<PostResponse> results = postService.searchPosts("java");

		assertEquals(1, results.size());
	}

	@Test
	@DisplayName("searchPosts — blank keyword returns empty list without DB call")
	void testSearchPosts_BlankKeyword() {
		List<PostResponse> results = postService.searchPosts("   ");
		assertTrue(results.isEmpty());
		verify(postRepository, never()).searchByContent(anyString());
	}

	@Test
	@DisplayName("searchPosts — null keyword returns empty list without DB call")
	void testSearchPosts_NullKeyword() {
		List<PostResponse> results = postService.searchPosts(null);
		assertTrue(results.isEmpty());
		verify(postRepository, never()).searchByContent(anyString());
	}

	@Test
	@DisplayName("incrementLikes — likesCount increases by 1")
	void testIncrementLikes() {
		when(postRepository.updateLikesCount(1, 1)).thenReturn(1);
		postService.incrementLikes(1);
		verify(postRepository).updateLikesCount(1, 1);
	}

	@Test
	@DisplayName("decrementLikes — likesCount decreases by 1")
	void testDecrementLikes() {
		when(postRepository.updateLikesCount(1, -1)).thenReturn(1);
		postService.decrementLikes(1);
		verify(postRepository).updateLikesCount(1, -1);
	}

	@Test
	@DisplayName("incrementComments — commentsCount increases by 1")
	void testIncrementComments() {
		when(postRepository.updateCommentsCount(1, 1)).thenReturn(1);
		postService.incrementComments(1);
		verify(postRepository).updateCommentsCount(1, 1);
	}

	@Test
	@DisplayName("decrementComments — commentsCount never goes below 0")
	void testDecrementComments_FloorAtZero() {
		when(postRepository.updateCommentsCount(1, -1)).thenReturn(1);
		postService.decrementComments(1);
		verify(postRepository).updateCommentsCount(1, -1);
	}

	@Test
	@DisplayName("incrementShares — sharesCount increases by 1")
	void testIncrementShares() {
		when(postRepository.incrementSharesCount(1)).thenReturn(1);
		postService.incrementShares(1);
		verify(postRepository).incrementSharesCount(1);
	}

	@Test
	@DisplayName("changeVisibility — updates post visibility to PRIVATE")
	void testChangeVisibility_ToPrivate() {
		when(postRepository.findByPostIdAndIsDeletedFalse(1)).thenReturn(Optional.of(samplePost));
		when(postRepository.save(any())).thenReturn(samplePost);

		postService.changeVisibility(1, Visibility.PRIVATE);
		assertEquals(Visibility.PRIVATE, samplePost.getVisibility());
	}

	@Test
	@DisplayName("changeVisibility — non-existent postId throws PostNotFoundException")
	void testChangeVisibility_PostNotFound() {
		when(postRepository.findByPostIdAndIsDeletedFalse(999)).thenReturn(Optional.empty());
		assertThrows(PostNotFoundException.class, () -> postService.changeVisibility(999, Visibility.PUBLIC));
	}

	@Test
	@DisplayName("getPostCount — returns correct count for authorId")
	void testGetPostCount() {
		when(postRepository.countByAuthorIdAndIsDeletedFalse(100)).thenReturn(7L);
		int count = postService.getPostCount(100);
		assertEquals(7, count);
	}

	@Test
	@DisplayName("incrementLikes — non-existent post throws PostNotFoundException")
	void testIncrementLikes_NotFound() {
		when(postRepository.updateLikesCount(999, 1)).thenReturn(0);
		assertThrows(PostNotFoundException.class, () -> postService.incrementLikes(999));
	}

	@Test
	@DisplayName("decrementLikes — non-existent post throws PostNotFoundException")
	void testDecrementLikes_NotFound() {
		when(postRepository.updateLikesCount(999, -1)).thenReturn(0);
		assertThrows(PostNotFoundException.class, () -> postService.decrementLikes(999));
	}

	@Test
	@DisplayName("incrementComments — non-existent post throws PostNotFoundException")
	void testIncrementComments_NotFound() {
		when(postRepository.updateCommentsCount(999, 1)).thenReturn(0);
		assertThrows(PostNotFoundException.class, () -> postService.incrementComments(999));
	}

	@Test
	@DisplayName("decrementComments — non-existent post throws PostNotFoundException")
	void testDecrementComments_NotFound() {
		when(postRepository.updateCommentsCount(999, -1)).thenReturn(0);
		assertThrows(PostNotFoundException.class, () -> postService.decrementComments(999));
	}

	@Test
	@DisplayName("incrementShares — non-existent post throws PostNotFoundException")
	void testIncrementShares_NotFound() {
		when(postRepository.incrementSharesCount(999)).thenReturn(0);
		assertThrows(PostNotFoundException.class, () -> postService.incrementShares(999));
	}

	@Test
	@DisplayName("getPublicFeed — returns posts")
	void testGetPublicFeed() {
		when(postRepository.findPublicPostsWithCursor(any(), any(), any()))
				.thenReturn(List.of(samplePost));
		List<PostResponse> feed = postService.getPublicFeed(null, 10);
		assertEquals(1, feed.size());
	}

	@Test
	@DisplayName("sharePost — creates share and increments share count")
	void testSharePost_Success() {
		com.connectsphere.post_service.dto.SharePostRequest shareReq = new com.connectsphere.post_service.dto.SharePostRequest();
		shareReq.setAuthorId(200);
		shareReq.setShareComment("Great post!");

		when(postRepository.findByPostIdAndIsDeletedFalse(1)).thenReturn(Optional.of(samplePost));
		when(postRepository.save(any(Post.class))).thenAnswer(inv -> {
			Post p = inv.getArgument(0);
			p.setPostId(2);
			p.setCreatedAt(LocalDateTime.now());
			return p;
		});
		when(postRepository.incrementSharesCount(1)).thenReturn(1);

		PostResponse result = postService.sharePost(1, shareReq);

		assertNotNull(result);
		assertEquals(200, result.getAuthorId());
		verify(postRepository).incrementSharesCount(1);
	}

	@Test
	@DisplayName("sharePost — null comment defaults to 'Shared post'")
	void testSharePost_NullComment() {
		com.connectsphere.post_service.dto.SharePostRequest shareReq = new com.connectsphere.post_service.dto.SharePostRequest();
		shareReq.setAuthorId(200);
		shareReq.setShareComment(null);

		when(postRepository.findByPostIdAndIsDeletedFalse(1)).thenReturn(Optional.of(samplePost));
		when(postRepository.save(any(Post.class))).thenAnswer(inv -> {
			Post p = inv.getArgument(0);
			p.setPostId(3);
			p.setCreatedAt(LocalDateTime.now());
			return p;
		});
		when(postRepository.incrementSharesCount(1)).thenReturn(1);

		PostResponse result = postService.sharePost(1, shareReq);
		assertNotNull(result);
	}

	@Test
	@DisplayName("getSharesOfPost — returns shares list")
	void testGetSharesOfPost() {
		when(postRepository.findByPostIdAndIsDeletedFalse(1)).thenReturn(Optional.of(samplePost));
		when(postRepository.findByOriginalPostIdAndIsDeletedFalse(1)).thenReturn(List.of(samplePost));

		List<PostResponse> shares = postService.getSharesOfPost(1);
		assertEquals(1, shares.size());
	}

	@Test
	@DisplayName("bookmarkPost — saves bookmark")
	void testBookmarkPost_Success() {
		when(postRepository.findByPostIdAndIsDeletedFalse(1)).thenReturn(Optional.of(samplePost));
		when(bookmarkRepository.existsByUserIdAndPostId(100, 1)).thenReturn(false);

		postService.bookmarkPost(100, 1);

		verify(bookmarkRepository).save(any());
	}

	@Test
	@DisplayName("bookmarkPost — duplicate throws DuplicateBookmarkException")
	void testBookmarkPost_Duplicate() {
		when(postRepository.findByPostIdAndIsDeletedFalse(1)).thenReturn(Optional.of(samplePost));
		when(bookmarkRepository.existsByUserIdAndPostId(100, 1)).thenReturn(true);

		assertThrows(com.connectsphere.post_service.exception.DuplicateBookmarkException.class,
				() -> postService.bookmarkPost(100, 1));
	}

	@Test
	@DisplayName("removeBookmark — removes bookmark")
	void testRemoveBookmark_Success() {
		when(bookmarkRepository.existsByUserIdAndPostId(100, 1)).thenReturn(true);

		postService.removeBookmark(100, 1);

		verify(bookmarkRepository).deleteByUserIdAndPostId(100, 1);
	}

	@Test
	@DisplayName("removeBookmark — not found throws PostNotFoundException")
	void testRemoveBookmark_NotFound() {
		when(bookmarkRepository.existsByUserIdAndPostId(100, 1)).thenReturn(false);

		assertThrows(PostNotFoundException.class, () -> postService.removeBookmark(100, 1));
	}

	@Test
	@DisplayName("getBookmarkedPosts — returns bookmarked posts")
	void testGetBookmarkedPosts() {
		when(bookmarkRepository.findPostIdsByUserId(100)).thenReturn(List.of(1));
		when(postRepository.findByPostIdAndIsDeletedFalse(1)).thenReturn(Optional.of(samplePost));

		List<PostResponse> posts = postService.getBookmarkedPosts(100);
		assertEquals(1, posts.size());
	}

	@Test
	@DisplayName("getBookmarkedPosts — empty bookmarks returns empty list")
	void testGetBookmarkedPosts_Empty() {
		when(bookmarkRepository.findPostIdsByUserId(100)).thenReturn(List.of());

		List<PostResponse> posts = postService.getBookmarkedPosts(100);
		assertTrue(posts.isEmpty());
	}

	@Test
	@DisplayName("createPost — RabbitMQ failure doesn't prevent post creation")
	void testCreatePost_RabbitMQFailure() {
		CreatePostRequest req = new CreatePostRequest();
		req.setAuthorId(100);
		req.setContent("Test");
		req.setVisibility(Visibility.PUBLIC);

		when(postRepository.save(any(Post.class))).thenReturn(samplePost);
		doThrow(new RuntimeException("RabbitMQ down")).when(rabbitTemplate)
				.convertAndSend(anyString(), anyString(), any(Object.class));

		PostResponse result = postService.createPost(req);
		assertNotNull(result);
	}

	@Test
	@DisplayName("createPost — search indexing failure doesn't prevent post creation")
	void testCreatePost_SearchIndexFailure() {
		CreatePostRequest req = new CreatePostRequest();
		req.setAuthorId(100);
		req.setContent("Test");
		req.setVisibility(Visibility.PUBLIC);

		when(postRepository.save(any(Post.class))).thenReturn(samplePost);
		doThrow(new RuntimeException("search down")).when(searchServiceClient)
				.indexPost(anyString(), anyString(), anyMap());

		PostResponse result = postService.createPost(req);
		assertNotNull(result);
	}
}
