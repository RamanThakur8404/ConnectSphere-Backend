package com.connectsphere.like_service.repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.connectsphere.like_service.entity.Like;
import com.connectsphere.like_service.constant.TargetType;

@Repository
public interface LikeRepository extends JpaRepository<Like, Integer> {

    // Find like by user and target
    Optional<Like> findByUserIdAndTargetId(Integer userId, Integer targetId);

    // Get all likes by a user
    List<Like> findByUserId(Integer userId);

    // Get all likes for a target
    List<Like> findByTargetId(Integer targetId);

    // Check if like exists
    boolean existsByUserIdAndTargetId(Integer userId, Integer targetId);

    // Count total likes on a target
    int countByTargetId(Integer targetId);

    // Count likes based on target type (POST / COMMENT)
    int countByTargetIdAndTargetType(Integer targetId, TargetType targetType);

    // Delete like (unlike)
    void deleteByUserIdAndTargetId(Integer userId, Integer targetId);

    @Query("SELECT l.reactionType, COUNT(l) FROM Like l WHERE l.targetId = :targetId GROUP BY l.reactionType")
    List<Object[]> findReactionSummary(@Param("targetId") Integer targetId);

    // Batch query to find likes for multiple targets for a specific user
    List<Like> findByUserIdAndTargetIdInAndTargetType(Integer userId, List<Integer> targetIds, TargetType targetType);
}