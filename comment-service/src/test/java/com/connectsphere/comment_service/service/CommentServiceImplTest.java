package com.connectsphere.comment_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.connectsphere.comment_service.client.PostServiceClient;
import com.connectsphere.comment_service.dto.CommentRequestDTO;
import com.connectsphere.comment_service.dto.CommentResponseDTO;
import com.connectsphere.comment_service.dto.CommentUpdateDTO;
import com.connectsphere.comment_service.entity.Comment;
import com.connectsphere.comment_service.exception.CommentNotFoundException;
import com.connectsphere.comment_service.exception.InvalidCommentOperationException;
import com.connectsphere.comment_service.exception.UnauthorizedCommentAccessException;
import com.connectsphere.comment_service.mapper.CommentMapper;
import com.connectsphere.comment_service.repository.CommentRepository;

// Unit tests for {@link CommentServiceImpl}.
@ExtendWith(MockitoExtension.class)
@DisplayName("CommentServiceImpl — Unit Tests")
class CommentServiceImplTest {

	@Mock
	private CommentRepository commentRepository;
	@Mock
	private CommentMapper commentMapper;
	@Mock
	private PostServiceClient postServiceClient;

	@InjectMocks
	private CommentServiceImpl commentService;

	private Comment comment;
	private CommentRequestDTO requestDTO;
	private CommentResponseDTO responseDTO;

	@BeforeEach
	void setUp() {
		comment = new Comment();
		comment.setCommentId(1L);
		comment.setPostId(10L);
		comment.setAuthorId(100L);
		comment.setContent("Test comment");
		comment.setLikesCount(0);
		comment.setIsDeleted(false);
		comment.setCreatedAt(LocalDateTime.now());
		comment.setUpdatedAt(LocalDateTime.now());

		requestDTO = new CommentRequestDTO();
		requestDTO.setPostId(10L);
		requestDTO.setAuthorId(100L);
		requestDTO.setContent("Test comment");

		responseDTO = new CommentResponseDTO();
		responseDTO.setCommentId(1L);
		responseDTO.setPostId(10L);
		responseDTO.setAuthorId(100L);
		responseDTO.setContent("Test comment");
	}

	// =========================================================================
	// addComment
	// =========================================================================

	@Test
	@DisplayName("addComment — happy path saves comment and increments post counter")
	void addComment_ValidRequest_SavesCommentAndIncrementsCounter() {
		when(commentMapper.toEntity(any())).thenReturn(comment);
		when(commentRepository.save(any())).thenReturn(comment);
		when(commentMapper.toResponseDTO(any())).thenReturn(responseDTO);

		CommentResponseDTO result = commentService.addComment(requestDTO);

		assertThat(result).isNotNull();
		assertThat(result.getCommentId()).isEqualTo(1L);
		assertThat(result.getContent()).isEqualTo("Test comment");
		verify(commentRepository).save(any(Comment.class));
		verify(postServiceClient).incrementCommentCount(10);
	}

	@Test
	@DisplayName("addComment — valid reply (top-level parent) saves successfully")
	void addComment_ValidReply_SavesSuccessfully() {
		Comment parentComment = new Comment();
		parentComment.setCommentId(5L);
		parentComment.setPostId(10L);
		parentComment.setParentCommentId(null); // top-level
		parentComment.setIsDeleted(false);

		requestDTO.setParentCommentId(5L);

		when(commentRepository.findByCommentIdAndIsDeletedFalse(5L)).thenReturn(Optional.of(parentComment));
		when(commentMapper.toEntity(any())).thenReturn(comment);
		when(commentRepository.save(any())).thenReturn(comment);
		when(commentMapper.toResponseDTO(any())).thenReturn(responseDTO);

		CommentResponseDTO result = commentService.addComment(requestDTO);

		assertThat(result).isNotNull();
		verify(commentRepository).save(any());
	}

	@Test
	@DisplayName("addComment — reply to a reply throws InvalidCommentOperationException")
	void addComment_ReplyToReply_ThrowsInvalidCommentOperationException() {
		Comment nestedParent = new Comment();
		nestedParent.setCommentId(5L);
		nestedParent.setParentCommentId(2L); // already a reply
		nestedParent.setIsDeleted(false);

		requestDTO.setParentCommentId(5L);

		when(commentRepository.findByCommentIdAndIsDeletedFalse(5L)).thenReturn(Optional.of(nestedParent));

		assertThatThrownBy(() -> commentService.addComment(requestDTO))
				.isInstanceOf(InvalidCommentOperationException.class)
				.hasMessageContaining("Replies to replies are not allowed");

		verify(commentRepository, never()).save(any());
	}

	@Test
	@DisplayName("addComment — parent comment not found throws CommentNotFoundException")
	void addComment_ParentCommentNotFound_ThrowsCommentNotFoundException() {
		requestDTO.setParentCommentId(999L);

		when(commentRepository.findByCommentIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> commentService.addComment(requestDTO)).isInstanceOf(CommentNotFoundException.class);
	}

	@Test
	@DisplayName("addComment — Feign failure is swallowed, comment is still saved")
	void addComment_FeignFails_CommentStillSaved() {
		when(commentMapper.toEntity(any())).thenReturn(comment);
		when(commentRepository.save(any())).thenReturn(comment);
		when(commentMapper.toResponseDTO(any())).thenReturn(responseDTO);
		doThrow(new RuntimeException("post-service unavailable")).when(postServiceClient)
				.incrementCommentCount(anyInt());

		CommentResponseDTO result = commentService.addComment(requestDTO);

		assertThat(result).isNotNull(); // comment saved even though Feign failed
	}

	// =========================================================================
	// getCommentsByPost
	// =========================================================================

	@Test
	@DisplayName("getCommentsByPost — returns mapped list of comments")
	void getCommentsByPost_PostExists_ReturnsCommentList() {
		when(commentRepository.findTopLevelByPostId(10L)).thenReturn(List.of(comment));
		when(commentMapper.toResponseDTOList(any())).thenReturn(List.of(responseDTO));

		List<CommentResponseDTO> result = commentService.getCommentsByPost(10L);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getCommentId()).isEqualTo(1L);
	}

	@Test
	@DisplayName("getCommentsByPost — returns empty list when no comments exist")
	void getCommentsByPost_NoComments_ReturnsEmptyList() {
		when(commentRepository.findTopLevelByPostId(99L)).thenReturn(List.of());
		when(commentMapper.toResponseDTOList(any())).thenReturn(List.of());

		List<CommentResponseDTO> result = commentService.getCommentsByPost(99L);

		assertThat(result).isEmpty();
	}

	// =========================================================================
	// getCommentById
	// =========================================================================

	@Test
	@DisplayName("getCommentById — returns comment when found")
	void getCommentById_CommentExists_ReturnsComment() {
		when(commentRepository.findByCommentIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(comment));
		when(commentMapper.toResponseDTO(any())).thenReturn(responseDTO);

		CommentResponseDTO result = commentService.getCommentById(1L);

		assertThat(result.getCommentId()).isEqualTo(1L);
	}

	@Test
	@DisplayName("getCommentById — throws CommentNotFoundException when not found")
	void getCommentById_CommentNotFound_ThrowsCommentNotFoundException() {
		when(commentRepository.findByCommentIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> commentService.getCommentById(999L)).isInstanceOf(CommentNotFoundException.class)
				.hasMessageContaining("999");
	}

	// =========================================================================
	// getReplies
	// =========================================================================

	@Test
	@DisplayName("getReplies — returns replies when parent exists")
	void getReplies_ParentExists_ReturnsReplies() {
		when(commentRepository.findByCommentIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(comment));
		when(commentRepository.findByParentCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(1L))
				.thenReturn(List.of(comment));
		when(commentMapper.toResponseDTOList(any())).thenReturn(List.of(responseDTO));

		List<CommentResponseDTO> result = commentService.getReplies(1L);

		assertThat(result).hasSize(1);
	}

	@Test
	@DisplayName("getReplies — throws when parent comment not found")
	void getReplies_ParentNotFound_ThrowsCommentNotFoundException() {
		when(commentRepository.findByCommentIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> commentService.getReplies(999L)).isInstanceOf(CommentNotFoundException.class);
	}

	// =========================================================================
	// updateComment
	// =========================================================================

	@Test
	@DisplayName("updateComment — author can update own comment")
	void updateComment_ByAuthor_UpdatesSuccessfully() {
		CommentUpdateDTO updateDTO = new CommentUpdateDTO();
		updateDTO.setContent("Updated content");

		when(commentRepository.findByCommentIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(comment));
		when(commentRepository.save(any())).thenReturn(comment);
		when(commentMapper.toResponseDTO(any())).thenReturn(responseDTO);

		// authorId = 100L matches comment.authorId = 100L
		CommentResponseDTO result = commentService.updateComment(1L, updateDTO, 100L);

		assertThat(result).isNotNull();
		verify(commentRepository).save(any(Comment.class));
	}

	@Test
	@DisplayName("updateComment — non-author throws UnauthorizedCommentAccessException")
	void updateComment_ByNonAuthor_ThrowsUnauthorizedCommentAccessException() {
		CommentUpdateDTO updateDTO = new CommentUpdateDTO();
		updateDTO.setContent("Hacked content");

		when(commentRepository.findByCommentIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(comment));

		// userId = 999L does NOT match comment.authorId = 100L
		assertThatThrownBy(() -> commentService.updateComment(1L, updateDTO, 999L))
				.isInstanceOf(UnauthorizedCommentAccessException.class).hasMessageContaining("not authorized");

		verify(commentRepository, never()).save(any());
	}

	@Test
	@DisplayName("updateComment — comment not found throws CommentNotFoundException")
	void updateComment_CommentNotFound_ThrowsCommentNotFoundException() {
		when(commentRepository.findByCommentIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> commentService.updateComment(999L, new CommentUpdateDTO(), 100L))
				.isInstanceOf(CommentNotFoundException.class);
	}

	// =========================================================================
	// deleteComment
	// =========================================================================

	@Test
	@DisplayName("deleteComment — author can delete own comment")
	void deleteComment_ByAuthor_SoftDeletesAndDecrementsCounter() {
		when(commentRepository.findByCommentIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(comment));
		when(commentRepository.findByParentCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(1L)).thenReturn(List.of());

		commentService.deleteComment(1L, 100L, "USER");

		verify(commentRepository).deleteByCommentId(1L);
		verify(postServiceClient).decrementCommentCount(10);
	}

	@Test
	@DisplayName("deleteComment — ADMIN can delete any comment")
	void deleteComment_ByAdmin_SoftDeletesSuccessfully() {
		when(commentRepository.findByCommentIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(comment));
		when(commentRepository.findByParentCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(1L)).thenReturn(List.of());

		// adminId = 999L does NOT own comment, but role = ADMIN
		commentService.deleteComment(1L, 999L, "ADMIN");

		verify(commentRepository).deleteByCommentId(1L);
	}

	@Test
	@DisplayName("deleteComment — MODERATOR can delete any comment")
	void deleteComment_ByModerator_SoftDeletesSuccessfully() {
		when(commentRepository.findByCommentIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(comment));
		when(commentRepository.findByParentCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(1L)).thenReturn(List.of());

		commentService.deleteComment(1L, 999L, "MODERATOR");

		verify(commentRepository).deleteByCommentId(1L);
	}

	@Test
	@DisplayName("deleteComment — non-owner USER throws UnauthorizedCommentAccessException")
	void deleteComment_ByNonOwnerUser_ThrowsUnauthorizedCommentAccessException() {
		when(commentRepository.findByCommentIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(comment));

		assertThatThrownBy(() -> commentService.deleteComment(1L, 999L, "USER"))
				.isInstanceOf(UnauthorizedCommentAccessException.class).hasMessageContaining("not authorized");

		verify(commentRepository, never()).deleteByCommentId(any());
	}

	@Test
	@DisplayName("deleteComment — also soft-deletes all child replies")
	void deleteComment_WithReplies_AlsoDeletesReplies() {
		Comment reply1 = new Comment();
		reply1.setCommentId(2L);
		reply1.setPostId(10L);
		reply1.setParentCommentId(1L);
		reply1.setIsDeleted(false);

		when(commentRepository.findByCommentIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(comment));
		when(commentRepository.findByParentCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(1L))
				.thenReturn(List.of(reply1));

		commentService.deleteComment(1L, 100L, "USER");

		verify(commentRepository).deleteByCommentId(2L); // reply deleted
		verify(commentRepository).deleteByCommentId(1L); // parent deleted
		verify(postServiceClient, times(2)).decrementCommentCount(10);
	}

	@Test
	@DisplayName("deleteComment — Feign failure is swallowed, comment still deleted")
	void deleteComment_FeignFails_CommentStillDeleted() {
		when(commentRepository.findByCommentIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(comment));
		when(commentRepository.findByParentCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(1L)).thenReturn(List.of());
		doThrow(new RuntimeException("post-service down")).when(postServiceClient).decrementCommentCount(anyInt());

		commentService.deleteComment(1L, 100L, "USER");

		verify(commentRepository).deleteByCommentId(1L); // still deleted
	}

	// =========================================================================
	// likeComment / unlikeComment
	// =========================================================================

	@Test
	@DisplayName("likeComment — increments like count when comment exists")
	void likeComment_CommentExists_IncrementsLikeCount() {
		when(commentRepository.findByCommentIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(comment));

		commentService.likeComment(1L);

		verify(commentRepository).incrementLikesCount(1L);
	}

	@Test
	@DisplayName("likeComment — throws CommentNotFoundException for missing comment")
	void likeComment_CommentNotFound_ThrowsCommentNotFoundException() {
		when(commentRepository.findByCommentIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> commentService.likeComment(999L)).isInstanceOf(CommentNotFoundException.class);
	}

	@Test
	@DisplayName("unlikeComment — decrements like count when comment exists")
	void unlikeComment_CommentExists_DecrementsLikeCount() {
		when(commentRepository.findByCommentIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(comment));

		commentService.unlikeComment(1L);

		verify(commentRepository).decrementLikesCount(1L);
	}

	@Test
	@DisplayName("unlikeComment — throws CommentNotFoundException for missing comment")
	void unlikeComment_CommentNotFound_ThrowsCommentNotFoundException() {
		when(commentRepository.findByCommentIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> commentService.unlikeComment(999L)).isInstanceOf(CommentNotFoundException.class);
	}

	// =========================================================================
	// getCommentsByUser / getCommentCount
	// =========================================================================

	@Test
	@DisplayName("getCommentsByUser — returns user comments")
	void getCommentsByUser_UserExists_ReturnsComments() {
		when(commentRepository.findByAuthorIdAndIsDeletedFalse(100L)).thenReturn(List.of(comment));
		when(commentMapper.toResponseDTOList(any())).thenReturn(List.of(responseDTO));

		List<CommentResponseDTO> result = commentService.getCommentsByUser(100L);

		assertThat(result).hasSize(1);
	}

	@Test
	@DisplayName("getCommentCount — returns correct count for a post")
	void getCommentCount_PostExists_ReturnsCount() {
		when(commentRepository.countByPostIdAndIsDeletedFalse(10L)).thenReturn(5L);

		Long count = commentService.getCommentCount(10L);

		assertThat(count).isEqualTo(5L);
	}
}
