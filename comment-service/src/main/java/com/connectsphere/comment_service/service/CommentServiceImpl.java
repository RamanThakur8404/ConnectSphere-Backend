package com.connectsphere.comment_service.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.connectsphere.comment_service.client.PostServiceClient;
import com.connectsphere.comment_service.constant.ErrorMessages;
import com.connectsphere.comment_service.constant.LogMessages;
import com.connectsphere.comment_service.dto.CommentRequestDTO;
import com.connectsphere.comment_service.dto.CommentResponseDTO;
import com.connectsphere.comment_service.dto.CommentUpdateDTO;
import com.connectsphere.comment_service.entity.Comment;
import com.connectsphere.comment_service.exception.CommentNotFoundException;
import com.connectsphere.comment_service.exception.InvalidCommentOperationException;
import com.connectsphere.comment_service.exception.UnauthorizedCommentAccessException;
import com.connectsphere.comment_service.mapper.CommentMapper;
import com.connectsphere.comment_service.repository.CommentRepository;

import lombok.RequiredArgsConstructor;

// Core implementation of {@link CommentService}.
@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

	private static final Logger logger = LoggerFactory.getLogger(CommentServiceImpl.class);

	private final CommentRepository commentRepository;
	private final CommentMapper commentMapper;
	private final PostServiceClient postServiceClient;

	// -----------------------------------------------------------------------
	// Add Comment
	// -----------------------------------------------------------------------

	@Override
	public CommentResponseDTO addComment(CommentRequestDTO requestDTO) {
		logger.info(LogMessages.ADD_COMMENT_ATTEMPT, requestDTO.getPostId(), requestDTO.getAuthorId(),
				requestDTO.getParentCommentId());

		// If it's a reply, validate parent exists and is a top-level comment
		if (requestDTO.getParentCommentId() != null) {
			Comment parentComment = commentRepository.findByCommentIdAndIsDeletedFalse(requestDTO.getParentCommentId())
					.orElseThrow(() -> new CommentNotFoundException(requestDTO.getParentCommentId()));

			if (parentComment.getParentCommentId() != null) {
				throw new InvalidCommentOperationException(ErrorMessages.REPLY_TO_REPLY_NOT_ALLOWED);
			}
		}

		Comment comment = commentMapper.toEntity(requestDTO);
		Comment savedComment = commentRepository.save(comment);

		// Feign call — non-critical; failure logs a warning but doesn't fail the
		// comment creation
		try {
			postServiceClient.incrementCommentCount(requestDTO.getPostId().intValue());
			logger.info(LogMessages.ADD_COMMENT_FEIGN_SUCCESS, requestDTO.getPostId());
		} catch (RuntimeException e) {
			logger.warn(LogMessages.ADD_COMMENT_FEIGN_FAILED, requestDTO.getPostId(), e.getMessage());
		}

		logger.info(LogMessages.ADD_COMMENT_SUCCESS, savedComment.getCommentId());
		return commentMapper.toResponseDTO(savedComment);
	}

	// -----------------------------------------------------------------------
	// Read Operations
	// -----------------------------------------------------------------------

	@Override
	@Transactional(readOnly = true)
	public List<CommentResponseDTO> getCommentsByPost(Long postId) {
		logger.info(LogMessages.GET_BY_POST_ATTEMPT, postId);
		return commentMapper.toResponseDTOList(commentRepository.findTopLevelByPostId(postId));
	}

	@Override
	@Transactional(readOnly = true)
	public CommentResponseDTO getCommentById(Long commentId) {
		logger.info(LogMessages.GET_BY_ID_ATTEMPT, commentId);
		Comment comment = commentRepository.findByCommentIdAndIsDeletedFalse(commentId)
				.orElseThrow(() -> new CommentNotFoundException(commentId));
		return commentMapper.toResponseDTO(comment);
	}

	@Override
	@Transactional(readOnly = true)
	public List<CommentResponseDTO> getReplies(Long parentCommentId) {
		logger.info(LogMessages.GET_REPLIES_ATTEMPT, parentCommentId);
		// Verify parent exists
		commentRepository.findByCommentIdAndIsDeletedFalse(parentCommentId)
				.orElseThrow(() -> new CommentNotFoundException(parentCommentId));
		return commentMapper.toResponseDTOList(
				commentRepository.findByParentCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(parentCommentId));
	}

	@Override
	@Transactional(readOnly = true)
	public List<CommentResponseDTO> getCommentsByUser(Long authorId) {
		logger.info(LogMessages.GET_BY_USER_ATTEMPT, authorId);
		return commentMapper.toResponseDTOList(commentRepository.findByAuthorIdAndIsDeletedFalse(authorId));
	}

	@Override
	@Transactional(readOnly = true)
	public Long getCommentCount(Long postId) {
		logger.info(LogMessages.GET_COUNT_ATTEMPT, postId);
		return commentRepository.countByPostIdAndIsDeletedFalse(postId);
	}

	// -----------------------------------------------------------------------
	// Update Comment — author only
	// -----------------------------------------------------------------------

	@Override
	public CommentResponseDTO updateComment(Long commentId, CommentUpdateDTO updateDTO, Long requestingUserId) {
		logger.info(LogMessages.UPDATE_COMMENT_ATTEMPT, commentId, requestingUserId);

		Comment comment = commentRepository.findByCommentIdAndIsDeletedFalse(commentId)
				.orElseThrow(() -> new CommentNotFoundException(commentId));

		// Ownership check — only the author can update
		if (!comment.getAuthorId().equals(requestingUserId)) {
			logger.warn(LogMessages.UNAUTHORIZED_ATTEMPT, "update", commentId, requestingUserId);
			throw new UnauthorizedCommentAccessException(ErrorMessages.UNAUTHORIZED_UPDATE);
		}

		comment.setContent(updateDTO.getContent());
		Comment updated = commentRepository.save(comment);

		logger.info(LogMessages.UPDATE_COMMENT_SUCCESS, commentId);
		return commentMapper.toResponseDTO(updated);
	}

	// -----------------------------------------------------------------------
	// Delete Comment — author / admin / moderator
	// -----------------------------------------------------------------------

	@Override
	public void deleteComment(Long commentId, Long requestingUserId, String requestingRole) {
		logger.info(LogMessages.DELETE_COMMENT_ATTEMPT, commentId, requestingUserId);

		Comment comment = commentRepository.findByCommentIdAndIsDeletedFalse(commentId)
				.orElseThrow(() -> new CommentNotFoundException(commentId));

		boolean isOwner = comment.getAuthorId().equals(requestingUserId);
		boolean isPrivileged = "ADMIN".equals(requestingRole) || "MODERATOR".equals(requestingRole);

		if (!isOwner && !isPrivileged) {
			logger.warn(LogMessages.UNAUTHORIZED_ATTEMPT, "delete", commentId, requestingUserId);
			throw new UnauthorizedCommentAccessException(ErrorMessages.UNAUTHORIZED_DELETE);
		}

		Long postId = comment.getPostId();

		// Soft-delete all replies first
		List<Comment> replies = commentRepository.findByParentCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(commentId);
		replies.forEach(reply -> commentRepository.deleteByCommentId(reply.getCommentId()));

		// Soft-delete the comment itself
		commentRepository.deleteByCommentId(commentId);

		// Feign call — non-critical
		try {
			int deletedCommentsCount = replies.size() + 1;
			for (int i = 0; i < deletedCommentsCount; i++) {
				postServiceClient.decrementCommentCount(postId.intValue());
			}
			logger.info(LogMessages.DELETE_COMMENT_FEIGN_SUCCESS, postId);
		} catch (RuntimeException e) {
			logger.warn(LogMessages.DELETE_COMMENT_FEIGN_FAILED, postId, e.getMessage());
		}

		logger.info(LogMessages.DELETE_COMMENT_SUCCESS, commentId, replies.size());
	}

	// -----------------------------------------------------------------------
	// Like / Unlike
	// -----------------------------------------------------------------------

	@Override
	public void likeComment(Long commentId) {
		logger.info(LogMessages.LIKE_COMMENT_ATTEMPT, commentId);
		commentRepository.findByCommentIdAndIsDeletedFalse(commentId)
				.orElseThrow(() -> new CommentNotFoundException(commentId));
		commentRepository.incrementLikesCount(commentId);
		logger.info(LogMessages.LIKE_COMMENT_SUCCESS, commentId);
	}

	@Override
	public void unlikeComment(Long commentId) {
		logger.info(LogMessages.UNLIKE_COMMENT_ATTEMPT, commentId);
		commentRepository.findByCommentIdAndIsDeletedFalse(commentId)
				.orElseThrow(() -> new CommentNotFoundException(commentId));
		commentRepository.decrementLikesCount(commentId);
		logger.info(LogMessages.UNLIKE_COMMENT_SUCCESS, commentId);
	}
}
