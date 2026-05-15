package com.connectsphere.comment_service.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.connectsphere.comment_service.dto.CommentRequestDTO;
import com.connectsphere.comment_service.dto.CommentResponseDTO;
import com.connectsphere.comment_service.entity.Comment;

class CommentMapperTest {

    private CommentMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CommentMapper();
    }

    @Test
    @DisplayName("toEntity works correctly")
    void toEntity() {
        CommentRequestDTO req = new CommentRequestDTO();
        req.setPostId(10L);
        req.setAuthorId(100L);
        req.setContent("Hello");
        req.setParentCommentId(5L);

        Comment comment = mapper.toEntity(req);
        assertThat(comment.getPostId()).isEqualTo(10L);
        assertThat(comment.getAuthorId()).isEqualTo(100L);
        assertThat(comment.getContent()).isEqualTo("Hello");
        assertThat(comment.getParentCommentId()).isEqualTo(5L);
    }

    @Test
    @DisplayName("toResponseDTO works correctly")
    void toResponseDTO() {
        Comment comment = new Comment();
        comment.setCommentId(1L);
        comment.setPostId(10L);
        comment.setAuthorId(100L);
        comment.setContent("Hello");
        comment.setParentCommentId(5L);
        comment.setLikesCount(10);
        LocalDateTime now = LocalDateTime.now();
        comment.setCreatedAt(now);

        CommentResponseDTO dto = mapper.toResponseDTO(comment);
        assertThat(dto.getCommentId()).isEqualTo(1L);
        assertThat(dto.getPostId()).isEqualTo(10L);
        assertThat(dto.getAuthorId()).isEqualTo(100L);
        assertThat(dto.getContent()).isEqualTo("Hello");
        assertThat(dto.getParentCommentId()).isEqualTo(5L);
        assertThat(dto.getLikesCount()).isEqualTo(10);
        assertThat(dto.getCreatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("toResponseDTOList works correctly")
    void toResponseDTOList() {
        Comment comment1 = new Comment();
        comment1.setCommentId(1L);
        
        Comment comment2 = new Comment();
        comment2.setCommentId(2L);
        
        List<CommentResponseDTO> dtoList = mapper.toResponseDTOList(List.of(comment1, comment2));
        assertThat(dtoList).hasSize(2);
        assertThat(dtoList.get(0).getCommentId()).isEqualTo(1L);
        assertThat(dtoList.get(1).getCommentId()).isEqualTo(2L);
    }
}
