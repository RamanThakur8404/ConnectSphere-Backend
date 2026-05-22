package com.connectsphere.comment_service.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponseDTO {
    private Long commentId;
    private Long postId;
    private Long authorId;
    private String content;
    private Long parentCommentId;
    private Integer likesCount;
    private LocalDateTime createdAt;
}