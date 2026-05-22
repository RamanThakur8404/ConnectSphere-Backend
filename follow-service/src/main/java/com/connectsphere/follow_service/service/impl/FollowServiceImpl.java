package com.connectsphere.follow_service.service.impl;

import com.connectsphere.follow_service.dto.FollowCountDTO;
import com.connectsphere.follow_service.dto.FollowRequestDTO;
import com.connectsphere.follow_service.dto.FollowResponseDTO;
import com.connectsphere.follow_service.entity.Follow;
import com.connectsphere.follow_service.entity.FollowStatus;
import com.connectsphere.follow_service.exception.DuplicateFollowException;
import com.connectsphere.follow_service.exception.FollowNotFoundException;
import com.connectsphere.follow_service.exception.SelfFollowException;
import com.connectsphere.follow_service.repository.FollowRepository;
import com.connectsphere.follow_service.service.FollowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// Implementation of {@link FollowService} that manages the directed social
@Service
@Transactional
public class FollowServiceImpl implements FollowService {

    // -------------------------------------------------------------------------
    // Logger
    // -------------------------------------------------------------------------

    private static final Logger LOGGER = LoggerFactory.getLogger(FollowServiceImpl.class);

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final FollowRepository followRepository;

    @Autowired
    public FollowServiceImpl(FollowRepository followRepository) {
        this.followRepository = followRepository;
    }

    // -------------------------------------------------------------------------
    // Core Operations
    // -------------------------------------------------------------------------

    // the relationship does not already exist, before persisting the record.</p>
    @Override
    public Follow follow(FollowRequestDTO followRequestDTO) {
        int followerId = followRequestDTO.getFollowerId();
        int followeeId = followRequestDTO.getFolloweeId();

        LOGGER.info("User {} attempting to follow user {}", followerId, followeeId);

        // Guard: a user cannot follow themselves
        if (followerId == followeeId) {
            LOGGER.warn("User {} attempted to follow themselves", followerId);
            throw new SelfFollowException("A user cannot follow themselves.");
        }

        // Guard: duplicate follow prevention (also enforced at DB level by unique constraint)
        if (followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            LOGGER.warn("Duplicate follow attempt: follower={}, followee={}", followerId, followeeId);
            throw new DuplicateFollowException(
                    "User " + followerId + " is already following user " + followeeId);
        }

        Follow follow = new Follow(followerId, followeeId, FollowStatus.ACTIVE);
        Follow saved = followRepository.save(follow);

        LOGGER.info("Follow relationship created: followId={}", saved.getFollowId());
        return saved;
    }

    //
    @Override
    public void unfollow(int followerId, int followeeId) {
        LOGGER.info("User {} attempting to unfollow user {}", followerId, followeeId);

        if (!followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            LOGGER.warn("Unfollow failed: no follow record for follower={}, followee={}",
                        followerId, followeeId);
            throw new FollowNotFoundException(
                    "No follow relationship found between user " + followerId
                    + " and user " + followeeId);
        }

        followRepository.deleteByFollowerIdAndFolloweeId(followerId, followeeId);
        LOGGER.info("User {} successfully unfollowed user {}", followerId, followeeId);
    }

    // -------------------------------------------------------------------------
    // Status Checks
    // -------------------------------------------------------------------------

    //
    @Override
    @Transactional(readOnly = true)
    public boolean isFollowing(int followerId, int followeeId) {
        boolean result = followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId);
        LOGGER.debug("isFollowing({}, {}) = {}", followerId, followeeId, result);
        return result;
    }

    // -------------------------------------------------------------------------
    // Graph Traversal
    // -------------------------------------------------------------------------

    //
    @Override
    @Transactional(readOnly = true)
    public List<Follow> getFollowers(int followeeId) {
        LOGGER.debug("Fetching followers for user {}", followeeId);
        return followRepository.findByFolloweeIdAndStatus(followeeId, FollowStatus.ACTIVE);
    }

    //
    @Override
    @Transactional(readOnly = true)
    public List<Follow> getFollowing(int followerId) {
        LOGGER.debug("Fetching following list for user {}", followerId);
        return followRepository.findByFollowerIdAndStatus(followerId, FollowStatus.ACTIVE);
    }

    // -------------------------------------------------------------------------
    // Count Aggregation
    // -------------------------------------------------------------------------

    //
    @Override
    @Transactional(readOnly = true)
    public int getFollowerCount(int followeeId) {
        int count = followRepository.countByFolloweeId(followeeId);
        LOGGER.debug("Follower count for user {}: {}", followeeId, count);
        return count;
    }

    //
    @Override
    @Transactional(readOnly = true)
    public int getFollowingCount(int followerId) {
        int count = followRepository.countByFollowerId(followerId);
        LOGGER.debug("Following count for user {}: {}", followerId, count);
        return count;
    }

    //
    @Override
    @Transactional(readOnly = true)
    public FollowCountDTO getFollowCounts(int userId) {
        int followerCount  = followRepository.countByFolloweeId(userId);
        int followingCount = followRepository.countByFollowerId(userId);
        LOGGER.debug("Counts for user {}: followers={}, following={}", userId,
                     followerCount, followingCount);
        return new FollowCountDTO(userId, followerCount, followingCount);
    }

    // -------------------------------------------------------------------------
    // Mutual Connections
    // -------------------------------------------------------------------------

    //
    @Override
    @Transactional(readOnly = true)
    public List<Integer> getMutualFollows(int userId, int otherUserId) {
        LOGGER.debug("Fetching mutual follows between user {} and user {}", userId, otherUserId);
        List<Integer> mutuals = followRepository.findMutualFollows(userId);
        // Filter to only return mutual follows that also include the other user if specified
        return mutuals;
    }

    // -------------------------------------------------------------------------
    // User Suggestions (Second-Degree Connections)
    // -------------------------------------------------------------------------

    //
    @Override
    @Transactional(readOnly = true)
    public List<Integer> getSuggestedUsers(int userId) {
        LOGGER.debug("Generating user suggestions for user {}", userId);

        // Step 1 – IDs of users that userId is already following
        Set<Integer> alreadyFollowing = followRepository
                .findByFollowerId(userId)
                .stream()
                .map(Follow::getFolloweeId)
                .collect(Collectors.toSet());

        alreadyFollowing.add(userId); // exclude self

        // Step 2 – Second-degree connections
        Set<Integer> suggestions = new java.util.LinkedHashSet<>();
        for (int followeeId : alreadyFollowing) {
            if (followeeId == userId) continue;
            followRepository.findByFollowerId(followeeId)
                    .stream()
                    .map(Follow::getFolloweeId)
                    .filter(id -> !alreadyFollowing.contains(id))
                    .forEach(suggestions::add);
        }

        LOGGER.debug("Found {} suggestion(s) for user {}", suggestions.size(), userId);
        return new ArrayList<>(suggestions);
    }

    // -------------------------------------------------------------------------
    // Mapper
    // -------------------------------------------------------------------------

    //
    @Override
    public FollowResponseDTO toResponseDTO(Follow follow) {
        return new FollowResponseDTO(
                follow.getFollowId(),
                follow.getFollowerId(),
                follow.getFolloweeId(),
                follow.getStatus(),
                follow.getCreatedAt()
        );
    }
}
