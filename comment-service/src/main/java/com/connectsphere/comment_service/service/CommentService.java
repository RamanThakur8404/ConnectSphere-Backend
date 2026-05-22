package com.connectsphere.comment_service.service;

import java.util.List;
import java.util.Optional;

import com.connectsphere.comment_service.dto.CommentRequestDTO;
import com.connectsphere.comment_service.dto.CommentResponseDTO;
import com.connectsphere.comment_service.dto.CommentUpdateDTO;

public interface CommentService {
	CommentResponseDTO addComment(CommentRequestDTO requestDTO);

	List<CommentResponseDTO> getCommentsByPost(Long postId);

	CommentResponseDTO getCommentById(Long commentId);

	List<CommentResponseDTO> getReplies(Long parentCommentId);

	CommentResponseDTO updateComment(Long commentId, CommentUpdateDTO updateDTO, Long requestingUserId);

	void deleteComment(Long commentId, Long requestingUserId, String requestingRole);

	List<CommentResponseDTO> getCommentsByUser(Long authorId);

	void likeComment(Long commentId);

	void unlikeComment(Long commentId);

	Long getCommentCount(Long postId);
}
