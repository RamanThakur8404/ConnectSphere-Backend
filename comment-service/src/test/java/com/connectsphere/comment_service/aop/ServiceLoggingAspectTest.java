package com.connectsphere.comment_service.aop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import com.connectsphere.comment_service.client.PostServiceClient;
import com.connectsphere.comment_service.dto.CommentRequestDTO;
import com.connectsphere.comment_service.dto.CommentResponseDTO;
import com.connectsphere.comment_service.entity.Comment;
import com.connectsphere.comment_service.exception.CommentNotFoundException;
import com.connectsphere.comment_service.mapper.CommentMapper;
import com.connectsphere.comment_service.repository.CommentRepository;
import com.connectsphere.comment_service.service.CommentService;

// Integration test verifying that the AOP {@link ServiceLoggingAspect} is wired
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ServiceLoggingAspect — Integration Tests")
class ServiceLoggingAspectTest {

    @Autowired
    private CommentService commentService; // Spring-proxied (AOP applied)

    @MockBean private CommentRepository commentRepository;
    @MockBean private CommentMapper     commentMapper;
    @MockBean private PostServiceClient postServiceClient;

    @Test
    @DisplayName("AOP proxy is active — CommentService is wrapped in a Spring proxy")
    void commentService_IsWrappedInAopProxy() {
        // The bean should be a CGLIB proxy (not the raw CommentServiceImpl)
        String className = commentService.getClass().getName();
        assertThat(className).containsAnyOf("$$", "EnhancerBySpringCGLIB", "SpringProxy");
    }

    @Test
    @DisplayName("AOP does not break happy-path service execution")
    void addComment_ThroughAopProxy_ExecutesNormally() {
        // Arrange
        Comment comment = new Comment();
        comment.setCommentId(1L);
        comment.setPostId(10L);
        comment.setAuthorId(100L);
        comment.setIsDeleted(false);

        CommentResponseDTO responseDTO = new CommentResponseDTO();
        responseDTO.setCommentId(1L);

        CommentRequestDTO request = new CommentRequestDTO();
        request.setPostId(10L);
        request.setAuthorId(100L);
        request.setContent("Test comment");

        when(commentMapper.toEntity(any())).thenReturn(comment);
        when(commentRepository.save(any())).thenReturn(comment);
        when(commentMapper.toResponseDTO(any())).thenReturn(responseDTO);

        // Act — call goes through AOP proxy
        CommentResponseDTO result = commentService.addComment(request);

        // Assert — AOP did not swallow the result
        assertThat(result).isNotNull();
        assertThat(result.getCommentId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("AOP does not swallow exceptions — CommentNotFoundException propagates")
    void getCommentById_ThroughAopProxy_PropagatesException() {
        when(commentRepository.findByCommentIdAndIsDeletedFalse(999L))
                .thenReturn(Optional.empty());

        // Exception must propagate through the AOP @Around advice unchanged
        assertThatThrownBy(() -> commentService.getCommentById(999L))
                .isInstanceOf(CommentNotFoundException.class)
                .hasMessageContaining("999");
    }
}
