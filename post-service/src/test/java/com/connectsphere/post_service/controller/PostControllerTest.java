package com.connectsphere.post_service.controller;

import com.connectsphere.post_service.constants.PostType;
import com.connectsphere.post_service.constants.Visibility;
import com.connectsphere.post_service.dto.CreatePostRequest;
import com.connectsphere.post_service.dto.FeedRequest;
import com.connectsphere.post_service.dto.PostResponse;
import com.connectsphere.post_service.dto.SharePostRequest;
import com.connectsphere.post_service.dto.UpdatePostRequest;
import com.connectsphere.post_service.exception.GlobalExceptionHandler;
import com.connectsphere.post_service.exception.PostNotFoundException;
import com.connectsphere.post_service.service.PostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@WebMvcTest(PostController.class)
@Import(GlobalExceptionHandler.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class PostControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private PostService postService;

	@Autowired
	private ObjectMapper objectMapper;

	private PostResponse samplePostResponse;
	private CreatePostRequest sampleCreateReq;

	@BeforeEach
	void setUp() {
		samplePostResponse = PostResponse.builder()
				.postId(1)
				.authorId(100)
				.content("Hello ConnectSphere!")
				.postType(PostType.TEXT)
				.visibility(Visibility.PUBLIC)
				.likesCount(0)
				.commentsCount(0)
				.sharesCount(0)
				.mediaUrls(Collections.emptyList())
				.createdAt(LocalDateTime.now())
				.updatedAt(LocalDateTime.now())
				.build();

		sampleCreateReq = new CreatePostRequest();
		sampleCreateReq.setAuthorId(100);
		sampleCreateReq.setContent("Hello ConnectSphere!");
		sampleCreateReq.setVisibility(Visibility.PUBLIC);
		sampleCreateReq.setPostType(PostType.TEXT);
	}

	@Test
	@DisplayName("POST /api/v1/posts — valid post returns 201 CREATED")
	void testCreatePost_Returns201() throws Exception {
		when(postService.createPost(any(CreatePostRequest.class))).thenReturn(samplePostResponse);

		mockMvc.perform(post("/api/v1/posts").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(sampleCreateReq))).andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.postId", is(1))).andExpect(jsonPath("$.data.authorId", is(100)))
				.andExpect(jsonPath("$.data.visibility", is("PUBLIC")));
	}

	@Test
	@DisplayName("POST /api/v1/posts — X-User-Id header overrides request authorId")
	void testCreatePost_HeaderUserIdOverridesAuthorId() throws Exception {
		when(postService.createPost(any(CreatePostRequest.class))).thenReturn(samplePostResponse);

		mockMvc.perform(post("/api/v1/posts").contentType(MediaType.APPLICATION_JSON)
				.header("X-User-Id", "321")
				.content(objectMapper.writeValueAsString(sampleCreateReq))).andExpect(status().isCreated());

		ArgumentCaptor<CreatePostRequest> captor = ArgumentCaptor.forClass(CreatePostRequest.class);
		org.mockito.Mockito.verify(postService).createPost(captor.capture());
		org.assertj.core.api.Assertions.assertThat(captor.getValue().getAuthorId()).isEqualTo(321);
	}

	@Test
	@DisplayName("GET /api/v1/posts/1 — returns 200 OK with post")
	void testGetPostById_Found() throws Exception {
		when(postService.getPostById(1)).thenReturn(Optional.of(samplePostResponse));

		mockMvc.perform(get("/api/v1/posts/1")).andExpect(status().isOk())
			.andExpect(jsonPath("$.data.postId", is(1)))
			.andExpect(jsonPath("$.data.content", is("Hello ConnectSphere!")));
	}

	@Test
	@DisplayName("GET /api/v1/posts/99 — non-existent post returns 404")
	void testGetPostById_NotFound_Returns404() throws Exception {
		when(postService.getPostById(99)).thenThrow(new PostNotFoundException("Post not found for postId: 99"));

		mockMvc.perform(get("/api/v1/posts/99")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status", is(404))).andExpect(jsonPath("$.message", containsString("99")));
	}

	@Test
	@DisplayName("GET /api/v1/posts/user/100 — returns list of posts")
	void testGetPostsByUser_ReturnsList() throws Exception {
		when(postService.getPostsByUser(100)).thenReturn(List.of(samplePostResponse));

		mockMvc.perform(get("/api/v1/posts/user/100")).andExpect(status().isOk())
			.andExpect(jsonPath("$.data", hasSize(1)))
			.andExpect(jsonPath("$.data[0].authorId", is(100)));
	}

	@Test
	@DisplayName("POST /api/v1/posts/feed — returns feed for given request")
	void testGetFeedForUser_ReturnsFeed() throws Exception {
		when(postService.getPersonalisedFeed(any(FeedRequest.class))).thenReturn(List.of(samplePostResponse));

		FeedRequest req = new FeedRequest();
		req.setFolloweeIds(List.of(101, 102));
		req.setLimit(20);

		mockMvc.perform(post("/api/v1/posts/feed").contentType(MediaType.APPLICATION_JSON)
			.content(objectMapper.writeValueAsString(req)))
			.andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(1)));
	}

	@Test
	@DisplayName("DELETE /api/v1/posts/1 — returns 200 with confirmation message")
	void testDeletePost_Returns200() throws Exception {
		doNothing().when(postService).deletePost(1);

		mockMvc.perform(delete("/api/v1/posts/1")).andExpect(status().isOk())
				.andExpect(jsonPath("$.message", containsString("Post deleted successfully")));
	}

	@Test
	@DisplayName("GET /api/v1/posts/public — returns public feed")
	void testGetPublicFeed_ReturnsList() throws Exception {
		when(postService.getPublicFeed(null, 20)).thenReturn(List.of(samplePostResponse));

		mockMvc.perform(get("/api/v1/posts/public")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data", hasSize(1)))
				.andExpect(jsonPath("$.data[0].postId", is(1)));
	}

	@Test
	@DisplayName("PUT /api/v1/posts/1 — updates post and returns response")
	void testUpdatePost_ReturnsUpdatedPost() throws Exception {
		UpdatePostRequest request = new UpdatePostRequest();
		request.setContent("Updated content");
		request.setVisibility(Visibility.PRIVATE);

		PostResponse updatedResponse = PostResponse.builder()
				.postId(1)
				.authorId(100)
				.content("Updated content")
				.postType(PostType.TEXT)
				.visibility(Visibility.PRIVATE)
				.likesCount(0)
				.commentsCount(0)
				.sharesCount(0)
				.mediaUrls(Collections.emptyList())
				.createdAt(samplePostResponse.getCreatedAt())
				.updatedAt(samplePostResponse.getUpdatedAt())
				.build();

		when(postService.updatePost(org.mockito.ArgumentMatchers.eq(1), any(UpdatePostRequest.class)))
				.thenReturn(updatedResponse);

		mockMvc.perform(put("/api/v1/posts/1").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.content", is("Updated content")))
				.andExpect(jsonPath("$.data.visibility", is("PRIVATE")));
	}

	@Test
	@DisplayName("GET /api/v1/posts/search?keyword=java — returns matching posts")
	void testSearchPosts_ReturnsResults() throws Exception {
		when(postService.searchPosts("java")).thenReturn(List.of(samplePostResponse));

		mockMvc.perform(get("/api/v1/posts/search").param("keyword", "java")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data", hasSize(1)));
	}

	@Test
	@DisplayName("PUT /api/v1/posts/1/visibility?visibility=PRIVATE — returns 200")
	void testChangeVisibility_ToPrivate() throws Exception {
		doNothing().when(postService).changeVisibility(1, Visibility.PRIVATE);

		mockMvc.perform(put("/api/v1/posts/1/visibility").param("visibility", "PRIVATE")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.message", containsString("PRIVATE")));
	}

	@Test
	@DisplayName("PUT /api/v1/posts/1/visibility?visibility=FOLLOWERS_ONLY — returns 200")
	void testChangeVisibility_ToFollowersOnly() throws Exception {
		doNothing().when(postService).changeVisibility(1, Visibility.FOLLOWERS_ONLY);

		mockMvc.perform(put("/api/v1/posts/1/visibility").param("visibility", "FOLLOWERS_ONLY"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.data.message", containsString("FOLLOWERS_ONLY")));
	}

	@Test
	@DisplayName("POST /api/v1/posts/1/like — returns 200 with increment message")
	void testIncrementLikes_Returns200() throws Exception {
		doNothing().when(postService).incrementLikes(1);

		mockMvc.perform(post("/api/v1/posts/1/like")).andExpect(status().isOk())
				.andExpect(jsonPath("$.message", containsString("incremented")));
	}

	@Test
	@DisplayName("POST /api/v1/posts/1/unlike — returns 200 with decrement message")
	void testDecrementLikes_Returns200() throws Exception {
		doNothing().when(postService).decrementLikes(1);

		mockMvc.perform(post("/api/v1/posts/1/unlike")).andExpect(status().isOk())
				.andExpect(jsonPath("$.message", containsString("decremented")));
	}

	@Test
	@DisplayName("POST /api/v1/posts/1/comment — returns 200 OK")
	void testIncrementComments_Returns200() throws Exception {
		doNothing().when(postService).incrementComments(1);

		mockMvc.perform(post("/api/v1/posts/1/comment")).andExpect(status().isOk())
				.andExpect(jsonPath("$.message", containsString("incremented")));
	}

	@Test
	@DisplayName("POST /api/v1/posts/1/share-count — returns 200 with increment message")
	void testIncrementShares_Returns200() throws Exception {
		doNothing().when(postService).incrementShares(1);

		mockMvc.perform(post("/api/v1/posts/1/share-count")).andExpect(status().isOk())
				.andExpect(jsonPath("$.message", containsString("incremented")));
	}

	@Test
	@DisplayName("POST /api/v1/posts/1/uncomment — returns 200 with decrement message")
	void testDecrementComments_Returns200() throws Exception {
		doNothing().when(postService).decrementComments(1);

		mockMvc.perform(post("/api/v1/posts/1/uncomment")).andExpect(status().isOk())
				.andExpect(jsonPath("$.message", containsString("decremented")));
	}

	@Test
	@DisplayName("POST /api/v1/posts/1/share — X-User-Id header overrides share authorId")
	void testSharePost_HeaderUserIdOverridesAuthorId() throws Exception {
		SharePostRequest request = new SharePostRequest();
		request.setAuthorId(55);
		request.setShareComment("Re-sharing");
		request.setVisibility(Visibility.PUBLIC);

		when(postService.sharePost(org.mockito.ArgumentMatchers.eq(1), any(SharePostRequest.class)))
				.thenReturn(samplePostResponse);

		mockMvc.perform(post("/api/v1/posts/1/share").contentType(MediaType.APPLICATION_JSON)
				.header("X-User-Id", "777")
				.content(objectMapper.writeValueAsString(request))).andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.postId", is(1)));

		ArgumentCaptor<SharePostRequest> captor = ArgumentCaptor.forClass(SharePostRequest.class);
		org.mockito.Mockito.verify(postService).sharePost(org.mockito.ArgumentMatchers.eq(1), captor.capture());
		org.assertj.core.api.Assertions.assertThat(captor.getValue().getAuthorId()).isEqualTo(777);
	}

	@Test
	@DisplayName("GET /api/v1/posts/1/shares — returns reshared posts")
	void testGetSharesOfPost_ReturnsList() throws Exception {
		when(postService.getSharesOfPost(1)).thenReturn(List.of(samplePostResponse));

		mockMvc.perform(get("/api/v1/posts/1/shares")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data", hasSize(1)))
				.andExpect(jsonPath("$.data[0].postId", is(1)));
	}

	@Test
	@DisplayName("POST /api/v1/posts/1/bookmark — bookmarks post for header user")
	void testBookmarkPost_Returns200() throws Exception {
		doNothing().when(postService).bookmarkPost(123, 1);

		mockMvc.perform(post("/api/v1/posts/1/bookmark").header("X-User-Id", "123"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message", containsString("bookmarked")));
	}

	@Test
	@DisplayName("DELETE /api/v1/posts/1/bookmark — removes bookmark for header user")
	void testRemoveBookmark_Returns200() throws Exception {
		doNothing().when(postService).removeBookmark(123, 1);

		mockMvc.perform(delete("/api/v1/posts/1/bookmark").header("X-User-Id", "123"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message", containsString("Bookmark removed")));
	}

	@Test
	@DisplayName("GET /api/v1/posts/bookmarks/100 — returns bookmarked posts")
	void testGetBookmarkedPosts_ReturnsList() throws Exception {
		when(postService.getBookmarkedPosts(100)).thenReturn(List.of(samplePostResponse));

		mockMvc.perform(get("/api/v1/posts/bookmarks/100")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data", hasSize(1)))
				.andExpect(jsonPath("$.data[0].authorId", is(100)));
	}

	@Test
	@DisplayName("GET /api/v1/posts/count/100 — returns correct post count")
	void testGetPostCount_Returns200() throws Exception {
		when(postService.getPostCount(100)).thenReturn(5);

		mockMvc.perform(get("/api/v1/posts/count/100")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.postCount", is(5)));
	}

	@Test
	@DisplayName("GET /api/v1/posts/bookmarks - returns bookmarks for header user")
	void testGetMyBookmarkedPosts_ReturnsList() throws Exception {
		when(postService.getBookmarkedPosts(123)).thenReturn(List.of(samplePostResponse));

		mockMvc.perform(get("/api/v1/posts/bookmarks").header("X-User-Id", "123")).andExpect(status().isOk())
				.andExpect(jsonPath("$.data", hasSize(1)))
				.andExpect(jsonPath("$.data[0].postId", is(1)));
	}
}
