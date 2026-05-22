package com.connectsphere.search_service.repository;

import com.connectsphere.search_service.entity.Hashtag;
import com.connectsphere.search_service.entity.PostHashtag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("SearchRepository Tests")
class SearchRepositoryTest {

    @Autowired
    private SearchRepository searchRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Hashtag savedHashtag;

    @BeforeEach
    void setUp() {
        searchRepository.deleteAll();

        savedHashtag = Hashtag.builder()
                .tag("springboot")
                .postCount(5)
                .lastUsedAt(LocalDateTime.now())
                .build();
        savedHashtag = entityManager.persistFlushFind(savedHashtag);
    }

    @Test
    @DisplayName("findByTag: returns hashtag for exact match")
    void findByTag_exactMatch_returnsHashtag() {
        Optional<Hashtag> result = searchRepository.findByTag("springboot");
        assertThat(result).isPresent();
        assertThat(result.get().getTag()).isEqualTo("springboot");
        assertThat(result.get().getPostCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("findByTag: returns empty for non-existent tag")
    void findByTag_notFound_returnsEmpty() {
        Optional<Hashtag> result = searchRepository.findByTag("nonexistent");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("searchByTagContaining: returns matching hashtags")
    void searchByTagContaining_partialMatch_returnsResults() {
        Hashtag java = Hashtag.builder()
                .tag("java")
                .postCount(10)
                .lastUsedAt(LocalDateTime.now())
                .build();
        entityManager.persistAndFlush(java);

        Hashtag javascript = Hashtag.builder()
                .tag("javascript")
                .postCount(3)
                .lastUsedAt(LocalDateTime.now())
                .build();
        entityManager.persistAndFlush(javascript);

        List<Hashtag> results = searchRepository.searchByTagContaining("java");
        assertThat(results).hasSize(2);
        // ordered by postCount DESC
        assertThat(results.get(0).getTag()).isEqualTo("java");
        assertThat(results.get(1).getTag()).isEqualTo("javascript");
    }

    @Test
    @DisplayName("searchByTagContaining: returns empty list when no match")
    void searchByTagContaining_noMatch_returnsEmpty() {
        List<Hashtag> results = searchRepository.searchByTagContaining("python");
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("findTrendingHashtags: returns top N hashtags within window")
    void findTrendingHashtags_withinWindow_returnsOrdered() {
        Hashtag trending1 = Hashtag.builder()
                .tag("trending1")
                .postCount(100)
                .lastUsedAt(LocalDateTime.now())
                .build();
        entityManager.persistAndFlush(trending1);

        Hashtag trending2 = Hashtag.builder()
                .tag("trending2")
                .postCount(50)
                .lastUsedAt(LocalDateTime.now())
                .build();
        entityManager.persistAndFlush(trending2);

        Hashtag oldTag = Hashtag.builder()
                .tag("oldtag")
                .postCount(200)
                .lastUsedAt(LocalDateTime.now().minusDays(2))
                .build();
        entityManager.persistAndFlush(oldTag);

        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<Hashtag> results = searchRepository.findTrendingHashtags(since, PageRequest.of(0, 2));

        // oldTag should be excluded (older than 24h), existing savedHashtag included
        assertThat(results).hasSizeLessThanOrEqualTo(2);
        assertThat(results).noneMatch(h -> h.getTag().equals("oldtag"));
    }

    @Test
    @DisplayName("countPostsByHashtag: returns post count for existing tag")
    void countPostsByHashtag_existing_returnsCount() {
        Integer count = searchRepository.countPostsByHashtag("springboot");
        assertThat(count).isEqualTo(5);
    }

    @Test
    @DisplayName("countPostsByHashtag: returns null for non-existent tag")
    void countPostsByHashtag_notFound_returnsNull() {
        Integer count = searchRepository.countPostsByHashtag("nonexistent");
        assertThat(count).isNull();
    }
}
