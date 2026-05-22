package com.connectsphere.media_service.repository;

import com.connectsphere.media_service.constants.MediaTypes;
import com.connectsphere.media_service.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

// Spring Data JPA repository for {@link Media} entities.
@Repository
public interface MediaRepository extends JpaRepository<Media, Integer> {

	// Returns all non-deleted media records uploaded by a specific user.
	@Query("SELECT m FROM Media m WHERE m.uploaderId = :uploaderId AND m.isDeleted = false "
			+ "ORDER BY m.uploadedAt DESC")
	List<Media> findByUploaderId(@Param("uploaderId") Integer uploaderId);

	// Returns a single non-deleted media record by its primary key.
	@Query("SELECT m FROM Media m WHERE m.mediaId = :mediaId AND m.isDeleted = false")
	Optional<Media> findByMediaId(@Param("mediaId") Integer mediaId);

	// Returns all active media records linked to a specific post.
	@Query("SELECT m FROM Media m WHERE m.linkedPostId = :linkedPostId AND m.isDeleted = false "
			+ "ORDER BY m.uploadedAt ASC")
	List<Media> findByLinkedPostId(@Param("linkedPostId") Integer linkedPostId);

	// Returns all non-deleted media records of a specific type (IMAGE or VIDEO) for
	@Query("SELECT m FROM Media m WHERE m.uploaderId = :uploaderId "
			+ "AND m.mediaTypes = :mediaTypes AND m.isDeleted = false")
	List<Media> findByMediaType(@Param("uploaderId") Integer uploaderId, @Param("mediaTypes") MediaTypes mediaTypes);

	// Performs a soft-delete: sets {@code isDeleted = true} for the given media ID.
	@Modifying
	@Transactional
	@Query("UPDATE Media m SET m.isDeleted = true WHERE m.mediaId = :mediaId")
	void deleteByMediaId(@Param("mediaId") Integer mediaId);
}