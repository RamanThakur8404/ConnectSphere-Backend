package com.connectsphere.follow_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.connectsphere.follow_service.dto.FollowCountDTO;
import com.connectsphere.follow_service.dto.FollowRequestDTO;
import com.connectsphere.follow_service.entity.Follow;
import com.connectsphere.follow_service.entity.FollowStatus;
import com.connectsphere.follow_service.exception.DuplicateFollowException;
import com.connectsphere.follow_service.exception.FollowNotFoundException;
import com.connectsphere.follow_service.exception.SelfFollowException;
import com.connectsphere.follow_service.repository.FollowRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("FollowServiceImpl Unit Tests")
class FollowServiceImplTest {

    @Mock private FollowRepository followRepository;
    @InjectMocks private FollowServiceImpl followService;

    private Follow follow;
    private FollowRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        follow = new Follow(1, 2, FollowStatus.ACTIVE);
        follow.setFollowId(100);
        follow.setCreatedAt(LocalDateTime.now());

        requestDTO = new FollowRequestDTO();
        requestDTO.setFollowerId(1);
        requestDTO.setFolloweeId(2);
    }

    @Nested
    @DisplayName("Follow Tests")
    class FollowTests {

        @Test
        @DisplayName("follow - creates follow relationship")
        void follow_success() {
            when(followRepository.existsByFollowerIdAndFolloweeId(1, 2)).thenReturn(false);
            when(followRepository.save(any())).thenReturn(follow);

            Follow result = followService.follow(requestDTO);

            assertThat(result.getFollowerId()).isEqualTo(1);
            assertThat(result.getFolloweeId()).isEqualTo(2);
            verify(followRepository).save(any());
        }

        @Test
        @DisplayName("follow - throws on self-follow")
        void follow_selfFollow() {
            requestDTO.setFollowerId(1);
            requestDTO.setFolloweeId(1);

            assertThatThrownBy(() -> followService.follow(requestDTO))
                    .isInstanceOf(SelfFollowException.class);
        }

        @Test
        @DisplayName("follow - throws on duplicate follow")
        void follow_duplicate() {
            when(followRepository.existsByFollowerIdAndFolloweeId(1, 2)).thenReturn(true);

            assertThatThrownBy(() -> followService.follow(requestDTO))
                    .isInstanceOf(DuplicateFollowException.class);
        }
    }

    @Nested
    @DisplayName("Unfollow Tests")
    class UnfollowTests {

        @Test
        @DisplayName("unfollow - removes follow relationship")
        void unfollow_success() {
            when(followRepository.existsByFollowerIdAndFolloweeId(1, 2)).thenReturn(true);

            followService.unfollow(1, 2);

            verify(followRepository).deleteByFollowerIdAndFolloweeId(1, 2);
        }

        @Test
        @DisplayName("unfollow - throws when not following")
        void unfollow_notFound() {
            when(followRepository.existsByFollowerIdAndFolloweeId(1, 2)).thenReturn(false);

            assertThatThrownBy(() -> followService.unfollow(1, 2))
                    .isInstanceOf(FollowNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Query Tests")
    class QueryTests {

        @Test
        @DisplayName("isFollowing - returns true when following")
        void isFollowing_true() {
            when(followRepository.existsByFollowerIdAndFolloweeId(1, 2)).thenReturn(true);

            assertThat(followService.isFollowing(1, 2)).isTrue();
        }

        @Test
        @DisplayName("isFollowing - returns false when not following")
        void isFollowing_false() {
            when(followRepository.existsByFollowerIdAndFolloweeId(1, 2)).thenReturn(false);

            assertThat(followService.isFollowing(1, 2)).isFalse();
        }

        @Test
        @DisplayName("getFollowers - returns follower list")
        void getFollowers_success() {
            when(followRepository.findByFolloweeIdAndStatus(2, FollowStatus.ACTIVE))
                    .thenReturn(List.of(follow));

            List<Follow> followers = followService.getFollowers(2);

            assertThat(followers).hasSize(1);
        }

        @Test
        @DisplayName("getFollowing - returns following list")
        void getFollowing_success() {
            when(followRepository.findByFollowerIdAndStatus(1, FollowStatus.ACTIVE))
                    .thenReturn(List.of(follow));

            List<Follow> following = followService.getFollowing(1);

            assertThat(following).hasSize(1);
        }

        @Test
        @DisplayName("getFollowCounts - returns both counts")
        void getFollowCounts_success() {
            when(followRepository.countByFolloweeId(1)).thenReturn(10);
            when(followRepository.countByFollowerId(1)).thenReturn(5);

            FollowCountDTO counts = followService.getFollowCounts(1);

            assertThat(counts.getFollowerCount()).isEqualTo(10);
            assertThat(counts.getFollowingCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("getFollowerCount - returns follower count")
        void getFollowerCount_success() {
            when(followRepository.countByFolloweeId(2)).thenReturn(8);

            int count = followService.getFollowerCount(2);

            assertThat(count).isEqualTo(8);
        }

        @Test
        @DisplayName("getFollowingCount - returns following count")
        void getFollowingCount_success() {
            when(followRepository.countByFollowerId(1)).thenReturn(6);

            int count = followService.getFollowingCount(1);

            assertThat(count).isEqualTo(6);
        }

        @Test
        @DisplayName("getMutualFollows - returns repository mutual follows")
        void getMutualFollows_success() {
            when(followRepository.findMutualFollows(1)).thenReturn(List.of(2, 3));

            List<Integer> mutuals = followService.getMutualFollows(1, 99);

            assertThat(mutuals).containsExactly(2, 3);
            verify(followRepository).findMutualFollows(1);
        }
    }

    @Nested
    @DisplayName("Suggestions Tests")
    class SuggestionsTests {

        @Test
        @DisplayName("getSuggestedUsers - returns second-degree connections")
        void getSuggestedUsers_success() {
            Follow f1 = new Follow(1, 2, FollowStatus.ACTIVE);
            Follow f2 = new Follow(2, 3, FollowStatus.ACTIVE);

            when(followRepository.findByFollowerId(1)).thenReturn(List.of(f1));
            when(followRepository.findByFollowerId(2)).thenReturn(List.of(f2));

            List<Integer> suggestions = followService.getSuggestedUsers(1);

            assertThat(suggestions).contains(3);
            assertThat(suggestions).doesNotContain(1, 2);
        }

        @Test
        @DisplayName("getSuggestedUsers - returns empty when no second-degrees")
        void getSuggestedUsers_empty() {
            when(followRepository.findByFollowerId(1)).thenReturn(List.of());

            List<Integer> suggestions = followService.getSuggestedUsers(1);

            assertThat(suggestions).isEmpty();
        }

        @Test
        @DisplayName("getSuggestedUsers - excludes users already followed")
        void getSuggestedUsers_filtersExistingConnections() {
            Follow followsTwo = new Follow(1, 2, FollowStatus.ACTIVE);
            Follow followsThree = new Follow(1, 3, FollowStatus.ACTIVE);
            Follow secondDegreeDuplicate = new Follow(2, 3, FollowStatus.ACTIVE);
            Follow secondDegreeNew = new Follow(2, 4, FollowStatus.ACTIVE);
            Follow thirdDegreeEmpty = new Follow(3, 1, FollowStatus.ACTIVE);

            when(followRepository.findByFollowerId(1)).thenReturn(List.of(followsTwo, followsThree));
            when(followRepository.findByFollowerId(2))
                    .thenReturn(List.of(secondDegreeDuplicate, secondDegreeNew));
            when(followRepository.findByFollowerId(3)).thenReturn(List.of(thirdDegreeEmpty));

            List<Integer> suggestions = followService.getSuggestedUsers(1);

            assertThat(suggestions).containsExactly(4);
        }
    }

    @Nested
    @DisplayName("Mapper Tests")
    class MapperTests {

        @Test
        @DisplayName("toResponseDTO - maps follow entity to response DTO")
        void toResponseDTO_success() {
            var response = followService.toResponseDTO(follow);

            assertThat(response.getFollowId()).isEqualTo(100);
            assertThat(response.getFollowerId()).isEqualTo(1);
            assertThat(response.getFolloweeId()).isEqualTo(2);
            assertThat(response.getStatus()).isEqualTo(FollowStatus.ACTIVE);
            assertThat(response.getCreatedAt()).isEqualTo(follow.getCreatedAt());
        }
    }
}
