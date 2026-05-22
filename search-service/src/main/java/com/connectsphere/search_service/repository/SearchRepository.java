package com.connectsphere.search_service.repository;

import com.connectsphere.search_service.entity.Hashtag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// Spring Data JPA repository for the Hashtag entity.
@Repository
public interface SearchRepository extends JpaRepository<Hashtag, Integer> {

    // Finds a hashtag by its exact lowercase tag string.
    Optional<Hashtag> findByTag(String tag);

    // Returns hashtags whose tag string contains the given substring.
    @Query("SELECT h FROM Hashtag h WHERE LOWER(h.tag) LIKE LOWER(CONCAT('%', :fragment, '%')) ORDER BY h.postCount DESC")
    List<Hashtag> searchByTagContaining(@Param("fragment") String fragment);

    // Returns the top trending hashtags ordered by postCount descending,
    @Query("SELECT h FROM Hashtag h WHERE h.lastUsedAt >= :since ORDER BY h.postCount DESC")
    List<Hashtag> findTrendingHashtags(@Param("since") LocalDateTime since, Pageable pageable);

    // Counts the number of posts associated with a given tag.
    @Query("SELECT COALESCE(h.postCount, 0) FROM Hashtag h WHERE h.tag = :tag")
    Integer countPostsByHashtag(@Param("tag") String tag);
}
