package com.connectsphere.comment_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.connectsphere.comment_service.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

	List<Comment> findByPostIdAndIsDeletedFalseOrderByCreatedAtAsc(Long postId);

	List<Comment> findByAuthorIdAndIsDeletedFalse(Long authorId);

	Optional<Comment> findByCommentIdAndIsDeletedFalse(Long commentId);

	List<Comment> findByParentCommentIdAndIsDeletedFalseOrderByCreatedAtAsc(Long parentCommentId);

	@Query("""
			SELECT c FROM Comment c
			WHERE c.postId = :postId
			  AND c.parentCommentId IS NULL
			  AND c.isDeleted = false
			ORDER BY c.createdAt ASC
			""")
	List<Comment> findTopLevelByPostId(@Param("postId") Long postId);

	long countByPostIdAndIsDeletedFalse(Long postId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE Comment c SET c.isDeleted = true WHERE c.commentId = :commentId")
	void deleteByCommentId(@Param("commentId") Long commentId);

	@Modifying
	@Query("UPDATE Comment c SET c.likesCount = c.likesCount + 1 WHERE c.commentId = :commentId")
	void incrementLikesCount(@Param("commentId") Long commentId);

	@Modifying
    @Query("UPDATE Comment c SET c.likesCount = CASE WHEN c.likesCount > 0 THEN c.likesCount - 1 ELSE 0 END WHERE c.commentId = :commentId")
    void decrementLikesCount(@Param("commentId") Long commentId);
}
