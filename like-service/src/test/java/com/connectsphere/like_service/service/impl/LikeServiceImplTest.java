package com.connectsphere.like_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.connectsphere.like_service.client.CommentServiceClient;
import com.connectsphere.like_service.client.PostServiceClient;
import com.connectsphere.like_service.constant.ReactionType;
import com.connectsphere.like_service.constant.TargetType;
import com.connectsphere.like_service.dto.*;
import com.connectsphere.like_service.entity.Like;
import com.connectsphere.like_service.exception.DuplicateLikeException;
import com.connectsphere.like_service.exception.LikeNotFoundException;
import com.connectsphere.like_service.mapper.LikeMapper;
import com.connectsphere.like_service.repository.LikeRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("LikeServiceImpl Unit Tests")
class LikeServiceImplTest {

    @Mock private LikeRepository likeRepository;
    @Mock private LikeMapper likeMapper;
    @Mock private PostServiceClient postServiceClient;
    @Mock private CommentServiceClient commentServiceClient;
    @InjectMocks private LikeServiceImpl likeService;

    private Like like;
    private LikeRequestDTO requestDTO;
    private LikeResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        like = new Like();
        like.setLikeId(1);
        like.setUserId(100);
        like.setTargetId(10);
        like.setTargetType(TargetType.POST);
        like.setReactionType(ReactionType.LIKE);
        like.setCreatedAt(LocalDateTime.now());

        requestDTO = new LikeRequestDTO();
        requestDTO.setUserId(100);
        requestDTO.setTargetId(10);
        requestDTO.setTargetType(TargetType.POST);
        requestDTO.setReactionType(ReactionType.LIKE);

        responseDTO = new LikeResponseDTO();
        responseDTO.setLikeId(1);
        responseDTO.setUserId(100);
        responseDTO.setTargetId(10);
        responseDTO.setTargetType(TargetType.POST);
        responseDTO.setReactionType(ReactionType.LIKE);
    }

    @Nested
    @DisplayName("Like Target Tests")
    class LikeTargetTests {

        @Test
        @DisplayName("likeTarget POST - saves and calls PostServiceClient")
        void likeTarget_post_success() {
            when(likeRepository.existsByUserIdAndTargetId(100, 10)).thenReturn(false);
            when(likeMapper.toEntity(any())).thenReturn(like);
            when(likeRepository.save(any())).thenReturn(like);
            when(likeMapper.toResponseDTO(any())).thenReturn(responseDTO);

            LikeResponseDTO result = likeService.likeTarget(requestDTO);

            assertThat(result.getLikeId()).isEqualTo(1);
            verify(postServiceClient).incrementLikeCount(10);
            verify(commentServiceClient, never()).incrementLikeCount(any());
        }

        @Test
        @DisplayName("likeTarget COMMENT - saves and calls CommentServiceClient")
        void likeTarget_comment_success() {
            requestDTO.setTargetType(TargetType.COMMENT);
            like.setTargetType(TargetType.COMMENT);
            responseDTO.setTargetType(TargetType.COMMENT);

            when(likeRepository.existsByUserIdAndTargetId(100, 10)).thenReturn(false);
            when(likeMapper.toEntity(any())).thenReturn(like);
            when(likeRepository.save(any())).thenReturn(like);
            when(likeMapper.toResponseDTO(any())).thenReturn(responseDTO);

            LikeResponseDTO result = likeService.likeTarget(requestDTO);

            assertThat(result.getLikeId()).isEqualTo(1);
            verify(commentServiceClient).incrementLikeCount(10L);
            verify(postServiceClient, never()).incrementLikeCount(anyInt());
        }

        @Test
        @DisplayName("likeTarget - throws DuplicateLikeException")
        void likeTarget_duplicate() {
            when(likeRepository.existsByUserIdAndTargetId(100, 10)).thenReturn(true);

            assertThatThrownBy(() -> likeService.likeTarget(requestDTO))
                    .isInstanceOf(DuplicateLikeException.class);
        }
    }

    @Nested
    @DisplayName("Unlike Target Tests")
    class UnlikeTargetTests {

        @Test
        @DisplayName("unlikeTarget POST - removes and calls PostServiceClient")
        void unlikeTarget_post_success() {
            when(likeRepository.existsByUserIdAndTargetId(100, 10)).thenReturn(true);

            likeService.unlikeTarget(100, 10, TargetType.POST);

            verify(likeRepository).deleteByUserIdAndTargetId(100, 10);
            verify(postServiceClient).decrementLikeCount(10);
        }

        @Test
        @DisplayName("unlikeTarget - throws LikeNotFoundException when no reaction")
        void unlikeTarget_notFound() {
            when(likeRepository.existsByUserIdAndTargetId(100, 10)).thenReturn(false);

            assertThatThrownBy(() -> likeService.unlikeTarget(100, 10, TargetType.POST))
                    .isInstanceOf(LikeNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Query Tests")
    class QueryTests {

        @Test
        @DisplayName("hasLiked - returns true")
        void hasLiked_true() {
            when(likeRepository.existsByUserIdAndTargetId(100, 10)).thenReturn(true);
            assertThat(likeService.hasLiked(100, 10, TargetType.POST)).isTrue();
        }

        @Test
        @DisplayName("getLikesByTarget - returns list")
        void getLikesByTarget_success() {
            when(likeRepository.findByTargetId(10)).thenReturn(List.of(like));
            when(likeMapper.toResponseDTO(any())).thenReturn(responseDTO);

            List<LikeResponseDTO> result = likeService.getLikesByTarget(10, TargetType.POST);
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("getLikeCount - returns count")
        void getLikeCount_success() {
            when(likeRepository.countByTargetId(10)).thenReturn(5);
            assertThat(likeService.getLikeCount(10, TargetType.POST)).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("Change Reaction Tests")
    class ChangeReactionTests {

        @Test
        @DisplayName("changeReaction - updates reaction type")
        void changeReaction_success() {
            ChangeReactionDTO changeDTO = new ChangeReactionDTO();
            changeDTO.setUserId(100);
            changeDTO.setTargetId(10);
            changeDTO.setTargetType(TargetType.POST);
            changeDTO.setNewReactionType(ReactionType.LOVE);

            Like updated = new Like();
            updated.setLikeId(1);
            updated.setReactionType(ReactionType.LOVE);

            when(likeRepository.findByUserIdAndTargetId(100, 10)).thenReturn(Optional.of(like));
            when(likeRepository.save(any())).thenReturn(updated);
            when(likeMapper.toResponseDTO(any())).thenReturn(responseDTO);

            LikeResponseDTO result = likeService.changeReaction(changeDTO);

            assertThat(result).isNotNull();
            verify(likeRepository).save(any());
        }

        @Test
        @DisplayName("changeReaction - throws when no existing reaction")
        void changeReaction_notFound() {
            ChangeReactionDTO changeDTO = new ChangeReactionDTO();
            changeDTO.setUserId(100);
            changeDTO.setTargetId(10);
            changeDTO.setTargetType(TargetType.POST);
            changeDTO.setNewReactionType(ReactionType.LOVE);

            when(likeRepository.findByUserIdAndTargetId(100, 10)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> likeService.changeReaction(changeDTO))
                    .isInstanceOf(LikeNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Additional Coverage Tests")
    class AdditionalTests {

        @Test
        @DisplayName("unlikeTarget COMMENT - removes and calls CommentServiceClient")
        void unlikeTarget_comment_success() {
            when(likeRepository.existsByUserIdAndTargetId(100, 10)).thenReturn(true);

            likeService.unlikeTarget(100, 10, TargetType.COMMENT);

            verify(likeRepository).deleteByUserIdAndTargetId(100, 10);
            verify(commentServiceClient).decrementLikeCount(10L);
        }

        @Test
        @DisplayName("likeTarget - Feign failure doesn't prevent save")
        void likeTarget_feignFailure() {
            when(likeRepository.existsByUserIdAndTargetId(100, 10)).thenReturn(false);
            when(likeMapper.toEntity(any())).thenReturn(like);
            when(likeRepository.save(any())).thenReturn(like);
            when(likeMapper.toResponseDTO(any())).thenReturn(responseDTO);
            doThrow(new RuntimeException("post-service down")).when(postServiceClient).incrementLikeCount(anyInt());

            LikeResponseDTO result = likeService.likeTarget(requestDTO);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("unlikeTarget - Feign failure doesn't prevent unlike")
        void unlikeTarget_feignFailure() {
            when(likeRepository.existsByUserIdAndTargetId(100, 10)).thenReturn(true);
            doThrow(new RuntimeException("post-service down")).when(postServiceClient).decrementLikeCount(anyInt());

            likeService.unlikeTarget(100, 10, TargetType.POST);

            verify(likeRepository).deleteByUserIdAndTargetId(100, 10);
        }

        @Test
        @DisplayName("getLikesByUser - returns user likes")
        void getLikesByUser_success() {
            when(likeRepository.findByUserId(100)).thenReturn(List.of(like));
            when(likeMapper.toResponseDTO(any())).thenReturn(responseDTO);

            List<LikeResponseDTO> result = likeService.getLikesByUser(100);
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("getLikeCountByType - returns count")
        void getLikeCountByType_success() {
            when(likeRepository.countByTargetId(10)).thenReturn(3);

            int count = likeService.getLikeCountByType(10, TargetType.POST, ReactionType.LIKE);
            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("hasLiked - returns false")
        void hasLiked_false() {
            when(likeRepository.existsByUserIdAndTargetId(100, 10)).thenReturn(false);
            assertThat(likeService.hasLiked(100, 10, TargetType.POST)).isFalse();
        }

        @Test
        @DisplayName("getReactionSummary - returns aggregated summary")
        void getReactionSummary_success() {
            Object[] row1 = new Object[]{ReactionType.LIKE, 5L};
            Object[] row2 = new Object[]{ReactionType.LOVE, 3L};
            when(likeRepository.findReactionSummary(10)).thenReturn(List.of(row1, row2));

            ReactionSummaryDTO summary = likeService.getReactionSummary(10, TargetType.POST);

            assertThat(summary.getTargetId()).isEqualTo(10);
            assertThat(summary.getTotalCount()).isEqualTo(8);
            assertThat(summary.getReactionCounts()).containsEntry(ReactionType.LIKE, 5L);
            assertThat(summary.getReactionCounts()).containsEntry(ReactionType.LOVE, 3L);
        }

        @Test
        @DisplayName("getReactionSummary - empty returns zero total")
        void getReactionSummary_empty() {
            when(likeRepository.findReactionSummary(10)).thenReturn(List.of());

            ReactionSummaryDTO summary = likeService.getReactionSummary(10, TargetType.POST);

            assertThat(summary.getTotalCount()).isEqualTo(0);
            assertThat(summary.getReactionCounts()).isEmpty();
        }
    }
}
