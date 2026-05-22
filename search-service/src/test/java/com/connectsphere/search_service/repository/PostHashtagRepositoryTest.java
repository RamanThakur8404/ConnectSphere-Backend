package com.connectsphere.search_service.repository;

import com.connectsphere.search_service.entity.Hashtag;
import com.connectsphere.search_service.entity.PostHashtag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("PostHashtagRepository Tests")
class PostHashtagRepositoryTest {

    @Autowired
    private PostHashtagRepository postHashtagRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Hashtag savedHashtag;

    @BeforeEach
    void setUp() {
        postHashtagRepository.deleteAll();

        savedHashtag = Hashtag.builder()
                .tag("java")
                .postCount(1)
                .lastUsedAt(LocalDateTime.now())
                .build();
        savedHashtag = entityManager.persistFlushFind(savedHashtag);

        PostHashtag mapping = PostHashtag.builder()
                .postId(10)
                .hashtag(savedHashtag)
                .createdAt(LocalDateTime.now())
                .build();
        entityManager.persistAndFlush(mapping);
    }

    @Test
    @DisplayName("findByPostId: returns mappings for existing post")
    void findByPostId_existing_returnsMappings() {
        List<PostHashtag> results = postHashtagRepository.findByPostId(10);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getHashtag().getTag()).isEqualTo("java");
    }

    @Test
    @DisplayName("findByPostId: returns empty for unknown post")
    void findByPostId_unknown_returnsEmpty() {
        List<PostHashtag> results = postHashtagRepository.findByPostId(999);
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("existsByPostIdAndHashtagHashtagId: returns true for existing pair")
    void existsByPair_existing_returnsTrue() {
        boolean exists = postHashtagRepository
                .existsByPostIdAndHashtagHashtagId(10, savedHashtag.getHashtagId());
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("existsByPostIdAndHashtagHashtagId: returns false for unknown pair")
    void existsByPair_unknown_returnsFalse() {
        boolean exists = postHashtagRepository
                .existsByPostIdAndHashtagHashtagId(999, savedHashtag.getHashtagId());
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("findPostsByHashtag: returns post IDs for a given tag")
    void findPostsByHashtag_existing_returnsPostIds() {
        List<Integer> postIds = postHashtagRepository.findPostsByHashtag("java");
        assertThat(postIds).containsExactly(10);
    }

    @Test
    @DisplayName("findPostsByHashtag: returns empty for unknown tag")
    void findPostsByHashtag_unknown_returnsEmpty() {
        List<Integer> postIds = postHashtagRepository.findPostsByHashtag("python");
        assertThat(postIds).isEmpty();
    }

    @Test
    @DisplayName("findHashtagIdsByPostId: returns hashtag IDs for a post")
    void findHashtagIdsByPostId_existing_returnsIds() {
        List<Integer> ids = postHashtagRepository.findHashtagIdsByPostId(10);
        assertThat(ids).containsExactly(savedHashtag.getHashtagId());
    }

    @Test
    @DisplayName("deleteByPostId: removes all mappings for a post")
    void deleteByPostId_removes_allMappings() {
        postHashtagRepository.deleteByPostId(10);
        entityManager.flush();
        entityManager.clear();

        List<PostHashtag> results = postHashtagRepository.findByPostId(10);
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("findPostsByHashtag: most recent first")
    void findPostsByHashtag_orderedByCreatedAtDesc() {
        PostHashtag mapping2 = PostHashtag.builder()
                .postId(20)
                .hashtag(savedHashtag)
                .createdAt(LocalDateTime.now().plusMinutes(1))
                .build();
        entityManager.persistAndFlush(mapping2);

        List<Integer> postIds = postHashtagRepository.findPostsByHashtag("java");
        assertThat(postIds).containsExactly(20, 10);
    }
}
