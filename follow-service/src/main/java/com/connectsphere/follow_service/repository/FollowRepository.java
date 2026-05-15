package com.connectsphere.follow_service.repository;

import com.connectsphere.follow_service.entity.Follow;
import com.connectsphere.follow_service.entity.FollowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// Spring Data JPA repository for {@link Follow} entities.
@Repository
public interface FollowRepository extends JpaRepository<Follow, Integer> {

    // Finds a specific follow record between two users, regardless of status.
    Optional<Follow> findByFollowerIdAndFolloweeId(int followerId, int followeeId);

    // Returns all users that the given user is following.
    List<Follow> findByFollowerId(int followerId);

    // Returns all users who follow the given user.
    List<Follow> findByFolloweeId(int followeeId);

    // Checks whether a directed follow relationship already exists.
    boolean existsByFollowerIdAndFolloweeId(int followerId, int followeeId);

    // Counts the number of users the given user is following.
    int countByFollowerId(int followerId);

    // Counts the number of followers for a given user.
    int countByFolloweeId(int followeeId);

    // Finds mutual follows (users that follow each other).
    @Query("""
           SELECT f1.followeeId
           FROM   Follow f1
           WHERE  f1.followerId = :userId
             AND  EXISTS (
                    SELECT 1 FROM Follow f2
                    WHERE  f2.followerId = f1.followeeId
                      AND  f2.followeeId = :userId
                  )
           """)
    List<Integer> findMutualFollows(@Param("userId") int userId);

    // Deletes the follow record between two specific users.
    void deleteByFollowerIdAndFolloweeId(int followerId, int followeeId);

    // Returns all ACTIVE followers for a given user.
    List<Follow> findByFolloweeIdAndStatus(int followeeId, FollowStatus status);

    // Returns all users the given user is actively following.
    List<Follow> findByFollowerIdAndStatus(int followerId, FollowStatus status);
}
