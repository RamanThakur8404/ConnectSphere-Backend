package com.connectsphere.follow_service.controller;

import com.connectsphere.follow_service.dto.FollowCountDTO;
import com.connectsphere.follow_service.dto.FollowRequestDTO;
import com.connectsphere.follow_service.dto.FollowResponseDTO;
import com.connectsphere.follow_service.entity.Follow;
import com.connectsphere.follow_service.service.FollowService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

// REST controller exposing all Follow-Service endpoints under {@code /api/v1/follows}.
@RestController
@RequestMapping("/api/v1/follows")
@Validated
public class FollowController {

    // -------------------------------------------------------------------------
    // Logger
    // -------------------------------------------------------------------------

    private static final Logger LOGGER = LoggerFactory.getLogger(FollowController.class);

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final FollowService followService;

    @Autowired
    public FollowController(FollowService followService) {
        this.followService = followService;
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/follows – Follow a user
    // -------------------------------------------------------------------------

    // Creates a new follow relationship.
    @PostMapping
    public ResponseEntity<FollowResponseDTO> follow(
            @Valid @RequestBody FollowRequestDTO followRequestDTO,
            Authentication authentication) {

        Integer authenticatedUserId = resolveAuthenticatedUserId(authentication);
        if (authenticatedUserId != null) {
            followRequestDTO.setFollowerId(authenticatedUserId);
        }

        LOGGER.info("POST /api/v1/follows – follower={}, followee={}",
                    followRequestDTO.getFollowerId(), followRequestDTO.getFolloweeId());

        Follow created = followService.follow(followRequestDTO);
        FollowResponseDTO responseDTO = followService.toResponseDTO(created);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/follows/{followerId}/{followeeId} – Unfollow a user
    // -------------------------------------------------------------------------

    // Removes an existing follow relationship.
    @DeleteMapping("/{followerId}/{followeeId}")
    public ResponseEntity<Void> unfollow(
            @PathVariable @Positive(message = "Follower ID must be positive") int followerId,
            @PathVariable @Positive(message = "Followee ID must be positive") int followeeId,
            Authentication authentication) {

        Integer authenticatedUserId = resolveAuthenticatedUserId(authentication);
        int effectiveFollowerId = authenticatedUserId != null ? authenticatedUserId : followerId;

        LOGGER.info("DELETE /api/v1/follows/{}/{}", effectiveFollowerId, followeeId);
        followService.unfollow(effectiveFollowerId, followeeId);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/follows/status/{followerId}/{followeeId} – Is following?
    // -------------------------------------------------------------------------

    // Checks whether a directed follow relationship exists.
    @GetMapping("/status/{followerId}/{followeeId}")
    public ResponseEntity<Boolean> isFollowing(
            @PathVariable @Positive int followerId,
            @PathVariable @Positive int followeeId) {

        LOGGER.debug("GET /api/v1/follows/status/{}/{}", followerId, followeeId);
        boolean following = followService.isFollowing(followerId, followeeId);
        return ResponseEntity.ok(following);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/follows/followers/{userId} – List followers
    // -------------------------------------------------------------------------

    // Returns all users that follow the specified user.
    @GetMapping("/followers/{userId}")
    public ResponseEntity<List<FollowResponseDTO>> getFollowers(
            @PathVariable @Positive int userId) {

        LOGGER.debug("GET /api/v1/follows/followers/{}", userId);

        List<FollowResponseDTO> followers = followService.getFollowers(userId)
                .stream()
                .map(followService::toResponseDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(followers);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/follows/following/{userId} – List following
    // -------------------------------------------------------------------------

    // Returns all users that the specified user is following.
    @GetMapping("/following/{userId}")
    public ResponseEntity<List<FollowResponseDTO>> getFollowing(
            @PathVariable @Positive int userId) {

        LOGGER.debug("GET /api/v1/follows/following/{}", userId);

        List<FollowResponseDTO> following = followService.getFollowing(userId)
                .stream()
                .map(followService::toResponseDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(following);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/follows/counts/{userId} – Follower + following counts
    // -------------------------------------------------------------------------

    // Returns both the follower count and the following count for a user.
    @GetMapping("/counts/{userId}")
    public ResponseEntity<FollowCountDTO> getFollowCounts(
            @PathVariable @Positive int userId) {

        LOGGER.debug("GET /api/v1/follows/counts/{}", userId);
        FollowCountDTO counts = followService.getFollowCounts(userId);
        return ResponseEntity.ok(counts);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/follows/follower-count/{userId}
    // -------------------------------------------------------------------------

    // Returns the follower count for a specific user.
    @GetMapping("/follower-count/{userId}")
    public ResponseEntity<Integer> getFollowerCount(
            @PathVariable @Positive int userId) {

        LOGGER.debug("GET /api/v1/follows/follower-count/{}", userId);
        return ResponseEntity.ok(followService.getFollowerCount(userId));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/follows/following-count/{userId}
    // -------------------------------------------------------------------------

    // Returns the following count for a specific user.
    @GetMapping("/following-count/{userId}")
    public ResponseEntity<Integer> getFollowingCount(
            @PathVariable @Positive int userId) {

        LOGGER.debug("GET /api/v1/follows/following-count/{}", userId);
        return ResponseEntity.ok(followService.getFollowingCount(userId));
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/follows/mutual/{userId}/{otherUserId}
    // -------------------------------------------------------------------------

    // Returns the list of user IDs that mutually follow each other with the given user.
    @GetMapping("/mutual/{userId}/{otherUserId}")
    public ResponseEntity<List<Integer>> getMutualFollows(
            @PathVariable @Positive int userId,
            @PathVariable @Positive int otherUserId) {

        LOGGER.debug("GET /api/v1/follows/mutual/{}/{}", userId, otherUserId);
        List<Integer> mutuals = followService.getMutualFollows(userId, otherUserId);
        return ResponseEntity.ok(mutuals);
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/follows/suggested/{userId}
    // -------------------------------------------------------------------------

    // Returns a list of user IDs suggested for the given user to follow,
    @GetMapping("/suggested/{userId}")
    public ResponseEntity<List<Integer>> getSuggestedUsers(
            @PathVariable @Positive int userId) {

        LOGGER.debug("GET /api/v1/follows/suggested/{}", userId);
        List<Integer> suggestions = followService.getSuggestedUsers(userId);
        return ResponseEntity.ok(suggestions);
    }

    private Integer resolveAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return null;
        }

        try {
            return Integer.valueOf(authentication.getName());
        } catch (NumberFormatException ex) {
            LOGGER.warn("Unable to parse authenticated user id: {}", authentication.getName());
            return null;
        }
    }
}
