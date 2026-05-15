package com.connectsphere.comment_service.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.connectsphere.comment_service.dto.CommentRequestDTO;
import com.connectsphere.comment_service.dto.CommentResponseDTO;
import com.connectsphere.comment_service.entity.Comment;

@Component
public class CommentMapper {

   
    public Comment toEntity(CommentRequestDTO request) {
        return Comment.builder()
                .postId(request.getPostId())
                .authorId(request.getAuthorId())
                .content(request.getContent())
                .parentCommentId(request.getParentCommentId())
                .build();
    }

    public CommentResponseDTO toResponseDTO(Comment comment) {
        return CommentResponseDTO.builder()
                .commentId(comment.getCommentId())
                .postId(comment.getPostId())
                .authorId(comment.getAuthorId())
                .content(comment.getContent())
                .parentCommentId(comment.getParentCommentId())
                .likesCount(comment.getLikesCount())
                .createdAt(comment.getCreatedAt())
                .build();
    }
    
    public List<CommentResponseDTO> toResponseDTOList(List<Comment> comments) {
        return comments.stream()
                .map(this::toResponseDTO)
                .toList();
    }
}