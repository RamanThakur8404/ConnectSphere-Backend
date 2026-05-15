package com.connectsphere.follow_service.controller;

import com.connectsphere.follow_service.dto.FollowCountDTO;
import com.connectsphere.follow_service.dto.FollowRequestDTO;
import com.connectsphere.follow_service.dto.FollowResponseDTO;
import com.connectsphere.follow_service.entity.Follow;
import com.connectsphere.follow_service.entity.FollowStatus;
import com.connectsphere.follow_service.service.FollowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FollowController Unit Tests")
class FollowControllerTest {

    @Mock
    private FollowService followService;

    @InjectMocks
    private FollowController followController;

    private FollowRequestDTO requestDTO;
    private Follow follow;
    private FollowResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        requestDTO = new FollowRequestDTO(1, 2);

        follow = new Follow(1, 2, FollowStatus.ACTIVE);
        follow.setFollowId(101);
        follow.setCreatedAt(LocalDateTime.now());

        responseDTO = new FollowResponseDTO(
                101,
                1,
                2,
                FollowStatus.ACTIVE,
                follow.getCreatedAt()
        );
    }

    @Test
    @DisplayName("follow - returns created response")
    void follow_success() {
        when(followService.follow(requestDTO)).thenReturn(follow);
        when(followService.toResponseDTO(follow)).thenReturn(responseDTO);

        ResponseEntity<FollowResponseDTO> response = followController.follow(requestDTO, auth(1));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(responseDTO);
        verify(followService).follow(requestDTO);
        verify(followService).toResponseDTO(follow);
    }

    @Test
    @DisplayName("unfollow - returns no content")
    void unfollow_success() {
        ResponseEntity<Void> response = followController.unfollow(1, 2, auth(1));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(followService).unfollow(1, 2);
    }

    @Test
    @DisplayName("follow - uses authenticated user id over request body")
    void follow_usesAuthenticatedUserId() {
        requestDTO.setFollowerId(99);
        when(followService.follow(requestDTO)).thenReturn(follow);
        when(followService.toResponseDTO(follow)).thenReturn(responseDTO);

        followController.follow(requestDTO, auth(1));

        assertThat(requestDTO.getFollowerId()).isEqualTo(1);
        verify(followService).follow(requestDTO);
    }

    @Test
    @DisplayName("unfollow - uses authenticated user id over path")
    void unfollow_usesAuthenticatedUserId() {
        followController.unfollow(99, 2, auth(1));

        verify(followService).unfollow(1, 2);
    }

    @Test
    @DisplayName("isFollowing - returns follow state")
    void isFollowing_success() {
        when(followService.isFollowing(1, 2)).thenReturn(true);

        ResponseEntity<Boolean> response = followController.isFollowing(1, 2);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isTrue();
        verify(followService).isFollowing(1, 2);
    }

    @Test
    @DisplayName("getFollowers - returns mapped followers")
    void getFollowers_success() {
        when(followService.getFollowers(2)).thenReturn(List.of(follow));
        when(followService.toResponseDTO(follow)).thenReturn(responseDTO);

        ResponseEntity<List<FollowResponseDTO>> response = followController.getFollowers(2);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(responseDTO);
        verify(followService).getFollowers(2);
        verify(followService).toResponseDTO(follow);
    }

    @Test
    @DisplayName("getFollowing - returns mapped following list")
    void getFollowing_success() {
        when(followService.getFollowing(1)).thenReturn(List.of(follow));
        when(followService.toResponseDTO(follow)).thenReturn(responseDTO);

        ResponseEntity<List<FollowResponseDTO>> response = followController.getFollowing(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(responseDTO);
        verify(followService).getFollowing(1);
        verify(followService).toResponseDTO(follow);
    }

    @Test
    @DisplayName("getFollowCounts - returns counts dto")
    void getFollowCounts_success() {
        FollowCountDTO counts = new FollowCountDTO(1, 9, 4);
        when(followService.getFollowCounts(1)).thenReturn(counts);

        ResponseEntity<FollowCountDTO> response = followController.getFollowCounts(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(counts);
        verify(followService).getFollowCounts(1);
    }

    @Test
    @DisplayName("getFollowerCount - returns follower count")
    void getFollowerCount_success() {
        when(followService.getFollowerCount(2)).thenReturn(7);

        ResponseEntity<Integer> response = followController.getFollowerCount(2);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(7);
        verify(followService).getFollowerCount(2);
    }

    @Test
    @DisplayName("getFollowingCount - returns following count")
    void getFollowingCount_success() {
        when(followService.getFollowingCount(1)).thenReturn(5);

        ResponseEntity<Integer> response = followController.getFollowingCount(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(5);
        verify(followService).getFollowingCount(1);
    }

    @Test
    @DisplayName("getMutualFollows - returns mutual follow ids")
    void getMutualFollows_success() {
        when(followService.getMutualFollows(1, 2)).thenReturn(List.of(3, 4));

        ResponseEntity<List<Integer>> response = followController.getMutualFollows(1, 2);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(3, 4);
        verify(followService).getMutualFollows(1, 2);
    }

    @Test
    @DisplayName("getSuggestedUsers - returns suggestions")
    void getSuggestedUsers_success() {
        when(followService.getSuggestedUsers(1)).thenReturn(List.of(5, 6));

        ResponseEntity<List<Integer>> response = followController.getSuggestedUsers(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(5, 6);
        verify(followService).getSuggestedUsers(1);
    }

    private Authentication auth(int userId) {
        return new UsernamePasswordAuthenticationToken(String.valueOf(userId), null);
    }
}
