package com.connectsphere.media_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.connectsphere.media_service.entity.Story;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// Spring Data JPA repository for {@link Story} entities.
@Repository
public interface StoryRepository extends JpaRepository<Story, Integer> {

	// Retrieves all currently active stories created by a specific user, ordered by
	@Query("SELECT s FROM Story s WHERE s.authorId = :authorId AND s.isActive = true " + "ORDER BY s.createdAt DESC")
	List<Story> findByAuthorId(@Param("authorId") int authorId);

	// Retrieves a single story by its ID, only if it is still active.
	@Query("SELECT s FROM Story s WHERE s.storyId = :storyId AND s.isActive = true")
	Optional<Story> findActiveById(@Param("storyId") int storyId);

	// Retrieves all currently active stories from a set of users (followers' feed).
	@Query("SELECT s FROM Story s WHERE s.authorId IN :authorIds AND s.isActive = true " + "ORDER BY s.createdAt DESC")
	List<Story> findActiveStoriesByAuthorIds(@Param("authorIds") List<Integer> authorIds);

	// Atomically increments the view count for a specific story. Called each time a
	@Modifying
	@Query("UPDATE Story s SET s.viewsCount = s.viewsCount + 1 WHERE s.storyId = :storyId")
	void incrementViewCount(@Param("storyId") int storyId);

	// Expires (soft-deletes) all stories whose expiresAt timestamp is before or
	@Modifying
	@Query("UPDATE Story s SET s.isActive = false " + "WHERE s.expiresAt <= :cutoff AND s.isActive = true")
	int expireStoriesOlderThan(@Param("cutoff") LocalDateTime cutoff);

	// Soft-deletes a single story by flipping isActive to false. Used when the
	@Modifying
	@Query("UPDATE Story s SET s.isActive = false WHERE s.storyId = :storyId")
	void deactivateStory(@Param("storyId") int storyId);

	// Counts how many active stories a user currently has.
	@Query("SELECT COUNT(s) FROM Story s WHERE s.authorId = :authorId AND s.isActive = true")
	int countActiveByAuthorId(@Param("authorId") int authorId);
}
