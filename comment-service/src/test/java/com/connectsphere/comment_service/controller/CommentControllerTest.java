package com.connectsphere.comment_service.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.connectsphere.comment_service.dto.CommentRequestDTO;
import com.connectsphere.comment_service.dto.CommentResponseDTO;
import com.connectsphere.comment_service.dto.CommentUpdateDTO;
import com.connectsphere.comment_service.exception.CommentNotFoundException;
import com.connectsphere.comment_service.exception.UnauthorizedCommentAccessException;
import com.connectsphere.comment_service.security.GatewayAuthFilter;
import com.connectsphere.comment_service.service.CommentService;
import com.fasterxml.jackson.databind.ObjectMapper;

// REST layer tests for {@link CommentController}.
@WebMvcTest(controllers = CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("CommentController — REST Layer Tests")
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommentService commentService;

    @MockBean
    private GatewayAuthFilter gatewayAuthFilter;

    private static final String BASE_URL = "/api/v1/comments";
    private CommentResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        responseDTO = new CommentResponseDTO();
        responseDTO.setCommentId(1L);
        responseDTO.setPostId(10L);
        responseDTO.setAuthorId(100L);
        responseDTO.setContent("Test comment");
        responseDTO.setLikesCount(0);
        responseDTO.setCreatedAt(LocalDateTime.now());
    }

    // =========================================================================
    // POST /api/v1/comments — addComment
    // =========================================================================

    @Test
    @DisplayName("POST /comments — creates comment, returns 201")
    void addComment_ValidRequest_Returns201() throws Exception {
        CommentRequestDTO request = new CommentRequestDTO();
        request.setPostId(10L);
        request.setAuthorId(100L);
        request.setContent("Test comment");

        when(commentService.addComment(any())).thenReturn(responseDTO);

        mockMvc.perform(post(BASE_URL)
                .header("X-User-Id", "100")
                .header("X-User-Role", "USER")
                .requestAttr("X-User-Id", 100L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.commentId").value(1L))
                .andExpect(jsonPath("$.content").value("Test comment"));

        verify(commentService).addComment(any());
    }

    @Test
    @DisplayName("POST /comments — blank content returns 400")
    void addComment_BlankContent_Returns400() throws Exception {
        CommentRequestDTO request = new CommentRequestDTO();
        request.setPostId(10L);
        request.setAuthorId(100L);
        request.setContent(""); // blank — fails @NotBlank

        mockMvc.perform(post(BASE_URL)
                .header("X-User-Id", "100")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // =========================================================================
    // GET /api/v1/comments/post/{postId}
    // =========================================================================

    @Test
    @DisplayName("GET /comments/post/{postId} — returns list of comments")
    void getCommentsByPost_PostExists_Returns200WithList() throws Exception {
        when(commentService.getCommentsByPost(10L)).thenReturn(List.of(responseDTO));

        mockMvc.perform(get(BASE_URL + "/post/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].commentId").value(1L))
                .andExpect(jsonPath("$[0].postId").value(10L));
    }

    @Test
    @DisplayName("GET /comments/post/{postId} — returns empty list when no comments")
    void getCommentsByPost_NoComments_Returns200EmptyList() throws Exception {
        when(commentService.getCommentsByPost(99L)).thenReturn(List.of());

        mockMvc.perform(get(BASE_URL + "/post/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // =========================================================================
    // GET /api/v1/comments/{commentId}
    // =========================================================================

    @Test
    @DisplayName("GET /comments/{commentId} — returns comment when found")
    void getCommentById_CommentExists_Returns200() throws Exception {
        when(commentService.getCommentById(1L)).thenReturn(responseDTO);

        mockMvc.perform(get(BASE_URL + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentId").value(1L));
    }

    @Test
    @DisplayName("GET /comments/{commentId} — returns 404 when not found")
    void getCommentById_CommentNotFound_Returns404() throws Exception {
        when(commentService.getCommentById(999L))
                .thenThrow(new CommentNotFoundException(999L));

        mockMvc.perform(get(BASE_URL + "/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Comment not found with ID: 999"));
    }

    // =========================================================================
    // GET /api/v1/comments/{commentId}/replies
    // =========================================================================

    @Test
    @DisplayName("GET /comments/{commentId}/replies — returns replies")
    void getReplies_ParentExists_Returns200WithReplies() throws Exception {
        when(commentService.getReplies(1L)).thenReturn(List.of(responseDTO));

        mockMvc.perform(get(BASE_URL + "/1/replies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].commentId").value(1L));
    }

    // =========================================================================
    // PUT /api/v1/comments/{commentId}
    // =========================================================================

    @Test
    @DisplayName("PUT /comments/{commentId} — author updates own comment, returns 200")
    void updateComment_ByAuthor_Returns200() throws Exception {
        CommentUpdateDTO updateDTO = new CommentUpdateDTO();
        updateDTO.setContent("Updated content");

        when(commentService.updateComment(eq(1L), any(CommentUpdateDTO.class), eq(100L)))
                .thenReturn(responseDTO);

        mockMvc.perform(put(BASE_URL + "/1")
                .header("X-User-Id", "100")
                .header("X-User-Role", "USER")
                .requestAttr("X-User-Id", 100L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentId").value(1L));
    }

    @Test
    @DisplayName("PUT /comments/{commentId} — non-author returns 403")
    void updateComment_ByNonAuthor_Returns403() throws Exception {
        CommentUpdateDTO updateDTO = new CommentUpdateDTO();
        updateDTO.setContent("Hacked content");

        when(commentService.updateComment(eq(1L), any(), eq(999L)))
                .thenThrow(new UnauthorizedCommentAccessException("You are not authorized to update this comment."));

        mockMvc.perform(put(BASE_URL + "/1")
                .header("X-User-Id", "999")
                .requestAttr("X-User-Id", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // =========================================================================
    // DELETE /api/v1/comments/{commentId}
    // =========================================================================

    @Test
    @DisplayName("DELETE /comments/{commentId} — author deletes own comment, returns 204")
    void deleteComment_ByAuthor_Returns204() throws Exception {
        doNothing().when(commentService).deleteComment(1L, 100L, "USER");

        mockMvc.perform(delete(BASE_URL + "/1")
                .header("X-User-Id", "100")
                .requestAttr("X-User-Id", 100L)
                .header("X-User-Role", "USER"))
                .andExpect(status().isNoContent());

        verify(commentService).deleteComment(1L, 100L, "USER");
    }

    @Test
    @DisplayName("DELETE /comments/{commentId} — non-owner returns 403")
    void deleteComment_ByNonOwner_Returns403() throws Exception {
        doThrow(new UnauthorizedCommentAccessException("You are not authorized to delete this comment."))
                .when(commentService).deleteComment(1L, 999L, "USER");

        mockMvc.perform(delete(BASE_URL + "/1")
                .header("X-User-Id", "999")
                .requestAttr("X-User-Id", 999L)
                .header("X-User-Role", "USER"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // POST /comments/{commentId}/like  &  /unlike
    // =========================================================================

    @Test
    @DisplayName("POST /comments/{commentId}/like — returns 200")
    void likeComment_CommentExists_Returns200() throws Exception {
        doNothing().when(commentService).likeComment(1L);

        mockMvc.perform(post(BASE_URL + "/1/like")
                .header("X-User-Id", "100")
                .requestAttr("X-User-Id", 100L))
                .andExpect(status().isOk());

        verify(commentService).likeComment(1L);
    }

    @Test
    @DisplayName("POST /comments/{commentId}/unlike — returns 200")
    void unlikeComment_CommentExists_Returns200() throws Exception {
        doNothing().when(commentService).unlikeComment(1L);

        mockMvc.perform(post(BASE_URL + "/1/unlike")
                .header("X-User-Id", "100")
                .requestAttr("X-User-Id", 100L))
                .andExpect(status().isOk());

        verify(commentService).unlikeComment(1L);
    }

    @Test
    @DisplayName("POST /comments/{commentId}/like — 404 when comment not found")
    void likeComment_NotFound_Returns404() throws Exception {
        doThrow(new CommentNotFoundException(999L))
                .when(commentService).likeComment(999L);

        mockMvc.perform(post(BASE_URL + "/999/like")
                .header("X-User-Id", "100"))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // GET /comments/post/{postId}/count
    // =========================================================================

    @Test
    @DisplayName("GET /comments/post/{postId}/count — returns comment count")
    void getCommentCount_PostExists_ReturnsCount() throws Exception {
        when(commentService.getCommentCount(10L)).thenReturn(5L);

        mockMvc.perform(get(BASE_URL + "/post/10/count"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    // =========================================================================
    // GET /comments/user/{authorId}
    // =========================================================================

    @Test
    @DisplayName("GET /comments/user/{authorId} — returns user comments")
    void getCommentsByUser_UserExists_ReturnsComments() throws Exception {
        when(commentService.getCommentsByUser(100L)).thenReturn(List.of(responseDTO));

        mockMvc.perform(get(BASE_URL + "/user/100")
                .header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].authorId").value(100L));
    }
}
