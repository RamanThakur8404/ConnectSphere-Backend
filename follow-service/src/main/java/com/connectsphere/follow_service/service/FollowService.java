package com.connectsphere.follow_service.service;

import com.connectsphere.follow_service.dto.FollowCountDTO;
import com.connectsphere.follow_service.dto.FollowRequestDTO;
import com.connectsphere.follow_service.dto.FollowResponseDTO;
import com.connectsphere.follow_service.entity.Follow;

import java.util.List;

// Business contract for the Follow-Service.
public interface FollowService {

    // Creates a new directed follow relationship from follower → followee.
    Follow follow(FollowRequestDTO followRequestDTO);

    // Removes the directed follow relationship from follower → followee.
    void unfollow(int followerId, int followeeId);

    // Checks whether a directed follow relationship exists between two users.
    boolean isFollowing(int followerId, int followeeId);

    // Retrieves all users who follow the specified user.
    List<Follow> getFollowers(int followeeId);

    // Retrieves all users that the specified user is following.
    List<Follow> getFollowing(int followerId);

    // Returns the total number of followers for the specified user.
    int getFollowerCount(int followeeId);

    // Returns the total number of users that the specified user is following.
    int getFollowingCount(int followerId);

    // Returns both follower and following counts bundled in a single DTO.
    FollowCountDTO getFollowCounts(int userId);

    // Returns the list of user IDs that mutually follow the given user
    List<Integer> getMutualFollows(int userId, int otherUserId);

    // Generates a list of user IDs suggested for the given user to follow,
    List<Integer> getSuggestedUsers(int userId);

    // Converts a {@link Follow} entity to its corresponding response DTO.
    FollowResponseDTO toResponseDTO(Follow follow);
}
