package com.connectsphere.search_service.repository;

import com.connectsphere.search_service.entity.PostHashtag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface PostHashtagRepository extends JpaRepository<PostHashtag, Long> {

    // Returns all PostHashtag records for a given post.
    List<PostHashtag> findByPostId(Integer postId);

    // Checks whether a mapping already exists for the given post-hashtag pair.
    boolean existsByPostIdAndHashtagHashtagId(Integer postId, Integer hashtagId);

    // Returns all post IDs that are tagged with a specific hashtag.
    @Query("SELECT ph.postId FROM PostHashtag ph WHERE ph.hashtag.tag = :tag ORDER BY ph.createdAt DESC")
    List<Integer> findPostsByHashtag(@Param("tag") String tag);

    // Returns all hashtag IDs associated with a given post.
    @Query("SELECT ph.hashtag.hashtagId FROM PostHashtag ph WHERE ph.postId = :postId")
    List<Integer> findHashtagIdsByPostId(@Param("postId") Integer postId);

    // Deletes all PostHashtag mappings for a given post.
    @Modifying
    @Transactional
    @Query("DELETE FROM PostHashtag ph WHERE ph.postId = :postId")
    void deleteByPostId(@Param("postId") Integer postId);
}
