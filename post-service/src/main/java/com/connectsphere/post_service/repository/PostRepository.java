package com.connectsphere.post_service.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.connectsphere.post_service.constants.Visibility;
import com.connectsphere.post_service.entity.Post;

// Spring Data JPA repository for Post entity. All queries exclude soft-deleted
public interface PostRepository extends JpaRepository<Post, Integer> {

	// -------------------------------------------------------------------------
	// Single-post lookups
	// -------------------------------------------------------------------------
	Optional<Post> findByPostIdAndIsDeletedFalse(int postId);

	// -------------------------------------------------------------------------
	// Author-scoped queries
	// -------------------------------------------------------------------------
	List<Post> findByAuthorIdAndIsDeletedFalse(int authorId);

	List<Post> findByAuthorIdAndIsDeletedFalseOrderByCreatedAtDesc(int authorId);

	long countByAuthorIdAndIsDeletedFalse(int authorId);

	// -------------------------------------------------------------------------
	// Visibility-scoped queries
	// -------------------------------------------------------------------------
	List<Post> findByVisibilityAndIsDeletedFalse(Visibility visibility);

	// -------------------------------------------------------------------------
	// Public feed (for unauthenticated users) with cursor-based pagination
	// -------------------------------------------------------------------------
	@Query("SELECT p FROM Post p " + "WHERE p.isDeleted = false " + "AND p.visibility = 'PUBLIC' "
			+ "AND (p.scheduledPublishAt IS NULL OR p.scheduledPublishAt <= :now) "
			+ "AND (:cursor IS NULL OR p.postId < :cursor) " + "ORDER BY p.createdAt DESC")
	List<Post> findPublicPostsWithCursor(@Param("now") LocalDateTime now, @Param("cursor") Integer cursor,
			Pageable pageable);

	// -------------------------------------------------------------------------
	// Personalised feed — posts from followed users, cursor-based pagination
	// -------------------------------------------------------------------------
	@Query("SELECT p FROM Post p " + "WHERE p.isDeleted = false "
			+ "AND (p.visibility = 'PUBLIC' OR (p.authorId IN :userIds AND p.visibility = 'FOLLOWERS_ONLY')) "
			+ "AND (p.scheduledPublishAt IS NULL OR p.scheduledPublishAt <= :now) "
			+ "AND (:cursor IS NULL OR p.postId < :cursor) " + "ORDER BY p.createdAt DESC")
	List<Post> findFeedByUserIdsWithCursor(@Param("userIds") List<Integer> userIds, @Param("now") LocalDateTime now,
			@Param("cursor") Integer cursor, Pageable pageable);

	// Legacy — kept for backward compatibility with older service calls 
	@Query("SELECT p FROM Post p " + "WHERE p.authorId IN :userIds " + "AND p.isDeleted = false "
			+ "AND p.visibility IN ('PUBLIC', 'FOLLOWERS_ONLY') " + "ORDER BY p.createdAt DESC")
	List<Post> findFeedByUserIds(@Param("userIds") List<Integer> userIds);

	// -------------------------------------------------------------------------
	// Full-text content search (PUBLIC posts only)
	// -------------------------------------------------------------------------
	@Query("SELECT p FROM Post p " + "WHERE LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')) "
			+ "AND p.isDeleted = false " + "AND p.visibility = 'PUBLIC' " + "ORDER BY p.createdAt DESC")
	List<Post> searchByContent(@Param("keyword") String keyword);

	// -------------------------------------------------------------------------
	// Scheduled post publishing — find posts due for publishing
	// -------------------------------------------------------------------------
	@Query("SELECT p FROM Post p " + "WHERE p.scheduledPublishAt IS NOT NULL " + "AND p.scheduledPublishAt <= :now "
			+ "AND p.isDeleted = false")
	List<Post> findDueScheduledPosts(@Param("now") LocalDateTime now);

	// -------------------------------------------------------------------------
	// Shares — find all reshares of a given original post
	// -------------------------------------------------------------------------
	List<Post> findByOriginalPostIdAndIsDeletedFalse(int originalPostId);

	// -------------------------------------------------------------------------
	// Atomic counter updates (avoid read-modify-write in service for hot paths)
	// -------------------------------------------------------------------------
	@Modifying
	@Transactional
	@Query("UPDATE Post p SET p.likesCount = GREATEST(0, p.likesCount + :delta) WHERE p.postId = :postId AND p.isDeleted = false")
	int updateLikesCount(@Param("postId") int postId, @Param("delta") int delta);

	@Modifying
	@Transactional
	@Query("UPDATE Post p SET p.commentsCount = GREATEST(0, p.commentsCount + :delta) WHERE p.postId = :postId AND p.isDeleted = false")
	int updateCommentsCount(@Param("postId") int postId, @Param("delta") int delta);

	@Modifying
	@Transactional
	@Query("UPDATE Post p SET p.sharesCount = GREATEST(0, p.sharesCount + 1) WHERE p.postId = :postId AND p.isDeleted = false")
	int incrementSharesCount(@Param("postId") int postId);
}
