package com.connectsphere.post_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.connectsphere.post_service.entity.Bookmark;

// Repository for user bookmark (saved post) operations.
public interface BookmarkRepository extends JpaRepository<Bookmark, Integer> {

    boolean existsByUserIdAndPostId(int userId, int postId);

    Optional<Bookmark> findByUserIdAndPostId(int userId, int postId);

    void deleteByUserIdAndPostId(int userId, int postId);

    List<Bookmark> findByUserIdOrderByBookmarkedAtDesc(int userId);

    long countByUserId(int userId);

    // Fetch all post IDs that a user has bookmarked 
    @Query("SELECT b.postId FROM Bookmark b WHERE b.userId = :userId ORDER BY b.bookmarkedAt DESC")
    List<Integer> findPostIdsByUserId(@Param("userId") int userId);
}
