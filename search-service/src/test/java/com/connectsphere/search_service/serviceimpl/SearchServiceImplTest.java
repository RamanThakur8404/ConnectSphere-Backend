package com.connectsphere.search_service.serviceimpl;

import com.connectsphere.search_service.dto.HashtagResponseDto;
import com.connectsphere.search_service.dto.HashtagSummaryDto;
import com.connectsphere.search_service.entity.Hashtag;
import com.connectsphere.search_service.entity.PostHashtag;
import com.connectsphere.search_service.exception.HashtagNotFoundException;
import com.connectsphere.search_service.exception.InvalidSearchQueryException;
import com.connectsphere.search_service.repository.PostHashtagRepository;
import com.connectsphere.search_service.repository.SearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchServiceImpl Tests")
class SearchServiceImplTest {

    @Mock
    private SearchRepository searchRepository;

    @Mock
    private PostHashtagRepository postHashtagRepository;

    @InjectMocks
    private SearchServiceImpl searchService;

    private Hashtag sampleHashtag;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(searchService, "trendingTopN", 10);
        ReflectionTestUtils.setField(searchService, "trendingWindowHours", 24);

        sampleHashtag = Hashtag.builder()
                .hashtagId(1)
                .tag("java")
                .postCount(5)
                .lastUsedAt(LocalDateTime.now())
                .build();
    }

    // indexPost

    @Test
    @DisplayName("indexPost: extracts and persists hashtags from content")
    void indexPost_withHashtags_indexesThem() {
        when(postHashtagRepository.findByPostId(1)).thenReturn(Collections.emptyList());
        when(searchRepository.findByTag("java")).thenReturn(Optional.empty());
        when(searchRepository.save(any(Hashtag.class))).thenAnswer(inv -> {
            Hashtag h = inv.getArgument(0);
            h.setHashtagId(1);
            return h;
        });
        when(postHashtagRepository.existsByPostIdAndHashtagHashtagId(anyInt(), anyInt()))
                .thenReturn(false);

        List<String> tags = searchService.indexPost(1, "Learning #Java and #Spring", List.of());

        assertThat(tags).containsExactly("java", "spring");
        verify(searchRepository, times(2)).save(any(Hashtag.class));
        verify(postHashtagRepository, times(2)).save(any(PostHashtag.class));
    }

    @Test
    @DisplayName("indexPost: returns empty list when no hashtags in content")
    void indexPost_noHashtags_returnsEmpty() {
        when(postHashtagRepository.findByPostId(1)).thenReturn(Collections.emptyList());

        List<String> tags = searchService.indexPost(1, "No hashtags here", List.of());

        assertThat(tags).isEmpty();
        verify(searchRepository, never()).save(any());
    }

    @Test
    @DisplayName("indexPost: upserts existing hashtag by incrementing count")
    void indexPost_existingHashtag_incrementsCount() {
        when(postHashtagRepository.findByPostId(1)).thenReturn(Collections.emptyList());
        when(searchRepository.findByTag("java")).thenReturn(Optional.of(sampleHashtag));
        when(searchRepository.save(any(Hashtag.class))).thenReturn(sampleHashtag);
        when(postHashtagRepository.existsByPostIdAndHashtagHashtagId(anyInt(), anyInt()))
                .thenReturn(false);

        searchService.indexPost(1, "Love #Java", List.of());

        assertThat(sampleHashtag.getPostCount()).isEqualTo(6);
    }

    @Test
    @DisplayName("indexPost: deduplicates hashtags in content")
    void indexPost_duplicateHashtags_deduplicates() {
        when(postHashtagRepository.findByPostId(1)).thenReturn(Collections.emptyList());
        when(searchRepository.findByTag("java")).thenReturn(Optional.empty());
        when(searchRepository.save(any(Hashtag.class))).thenAnswer(inv -> {
            Hashtag h = inv.getArgument(0);
            h.setHashtagId(1);
            return h;
        });
        when(postHashtagRepository.existsByPostIdAndHashtagHashtagId(anyInt(), anyInt()))
                .thenReturn(false);

        List<String> tags = searchService.indexPost(1, "#Java #java #JAVA", List.of());

        assertThat(tags).containsExactly("java");
    }

    @Test
    @DisplayName("indexPost: persists provided backend hashtags")
    void indexPost_withProvidedTags_indexesThem() {
        when(postHashtagRepository.findByPostId(1)).thenReturn(Collections.emptyList());
        when(searchRepository.findByTag("frontend")).thenReturn(Optional.empty());
        when(searchRepository.findByTag("connectsphere")).thenReturn(Optional.empty());
        when(searchRepository.save(any(Hashtag.class))).thenAnswer(inv -> {
            Hashtag h = inv.getArgument(0);
            h.setHashtagId(1);
            return h;
        });
        when(postHashtagRepository.existsByPostIdAndHashtagHashtagId(anyInt(), anyInt()))
                .thenReturn(false);

        List<String> tags = searchService.indexPost(1, "No explicit tags", List.of("frontend", "#ConnectSphere"));

        assertThat(tags).containsExactly("frontend", "connectsphere");
        verify(postHashtagRepository, times(2)).save(any(PostHashtag.class));
    }

    // removePostIndex

    @Test
    @DisplayName("removePostIndex: removes mappings and decrements counts")
    void removePostIndex_existingMappings_removesAndDecrements() {
        PostHashtag mapping = PostHashtag.builder()
                .postId(1)
                .hashtag(sampleHashtag)
                .build();
        when(postHashtagRepository.findByPostId(1)).thenReturn(List.of(mapping));
        when(searchRepository.save(any(Hashtag.class))).thenReturn(sampleHashtag);

        searchService.removePostIndex(1);

        assertThat(sampleHashtag.getPostCount()).isEqualTo(4);
        verify(postHashtagRepository).deleteByPostId(1);
    }

    // searchPosts

    @Test
    @DisplayName("searchPosts: returns post IDs for matching hashtags")
    void searchPosts_matchingKeyword_returnsPostIds() {
        when(searchRepository.searchByTagContaining("java")).thenReturn(List.of(sampleHashtag));
        when(postHashtagRepository.findPostsByHashtag("java")).thenReturn(List.of(10, 20));

        List<Integer> result = searchService.searchPosts("java");

        assertThat(result).containsExactly(10, 20);
    }

    @Test
    @DisplayName("searchPosts: strips '#' prefix from keyword")
    void searchPosts_withHash_stripsPrefix() {
        when(searchRepository.searchByTagContaining("java")).thenReturn(List.of(sampleHashtag));
        when(postHashtagRepository.findPostsByHashtag("java")).thenReturn(List.of(10));

        List<Integer> result = searchService.searchPosts("#Java");

        assertThat(result).containsExactly(10);
    }

    @Test
    @DisplayName("searchPosts: returns empty for blank keyword")
    void searchPosts_blankKeyword_returnsEmpty() {
        List<Integer> result = searchService.searchPosts("");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("searchPosts: returns empty when no hashtags match")
    void searchPosts_noMatch_returnsEmpty() {
        when(searchRepository.searchByTagContaining("python")).thenReturn(Collections.emptyList());

        List<Integer> result = searchService.searchPosts("python");

        assertThat(result).isEmpty();
    }

    // searchUsers

    @Test
    @DisplayName("searchUsers: returns empty list (stub)")
    void searchUsers_stub_returnsEmpty() {
        List<Integer> result = searchService.searchUsers("john");
        assertThat(result).isEmpty();
    }

    // getHashtagsForPost

    @Test
    @DisplayName("getHashtagsForPost: returns hashtag DTOs for post")
    void getHashtagsForPost_existingPost_returnsDtos() {
        PostHashtag mapping = PostHashtag.builder()
                .postId(10)
                .hashtag(sampleHashtag)
                .build();
        when(postHashtagRepository.findByPostId(10)).thenReturn(List.of(mapping));

        List<HashtagResponseDto> result = searchService.getHashtagsForPost(10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTag()).isEqualTo("java");
        assertThat(result.get(0).getPostCount()).isEqualTo(5);
    }

    @Test
    @DisplayName("getHashtagsForPost: returns empty for post with no hashtags")
    void getHashtagsForPost_noHashtags_returnsEmpty() {
        when(postHashtagRepository.findByPostId(999)).thenReturn(Collections.emptyList());

        List<HashtagResponseDto> result = searchService.getHashtagsForPost(999);

        assertThat(result).isEmpty();
    }

    // getTrendingHashtags

    @Test
    @DisplayName("getTrendingHashtags: returns summary DTOs")
    void getTrendingHashtags_returnsOrdered() {
        when(searchRepository.findTrendingHashtags(any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(List.of(sampleHashtag));

        List<HashtagSummaryDto> result = searchService.getTrendingHashtags();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTag()).isEqualTo("java");
    }

    // getPostsByHashtag

    @Test
    @DisplayName("getPostsByHashtag: returns post IDs for existing tag")
    void getPostsByHashtag_existingTag_returnsIds() {
        when(searchRepository.findByTag("java")).thenReturn(Optional.of(sampleHashtag));
        when(postHashtagRepository.findPostsByHashtag("java")).thenReturn(List.of(10, 20));

        List<Integer> result = searchService.getPostsByHashtag("java");

        assertThat(result).containsExactly(10, 20);
    }

    @Test
    @DisplayName("getPostsByHashtag: throws when tag not found")
    void getPostsByHashtag_notFound_throws() {
        when(searchRepository.findByTag("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> searchService.getPostsByHashtag("unknown"))
                .isInstanceOf(HashtagNotFoundException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    @DisplayName("getPostsByHashtag: strips '#' prefix")
    void getPostsByHashtag_withHashPrefix_strips() {
        when(searchRepository.findByTag("java")).thenReturn(Optional.of(sampleHashtag));
        when(postHashtagRepository.findPostsByHashtag("java")).thenReturn(List.of(10));

        List<Integer> result = searchService.getPostsByHashtag("#Java");

        assertThat(result).containsExactly(10);
    }

    // searchHashtags

    @Test
    @DisplayName("searchHashtags: returns matching DTOs")
    void searchHashtags_matchingFragment_returnsDtos() {
        when(searchRepository.searchByTagContaining("jav")).thenReturn(List.of(sampleHashtag));

        List<HashtagResponseDto> result = searchService.searchHashtags("jav");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTag()).isEqualTo("java");
    }

    @Test
    @DisplayName("searchHashtags: returns empty for blank fragment")
    void searchHashtags_blankFragment_returnsEmpty() {
        List<HashtagResponseDto> result = searchService.searchHashtags("");
        assertThat(result).isEmpty();
    }

    // getHashtagCount

    @Test
    @DisplayName("getHashtagCount: returns count for existing tag")
    void getHashtagCount_existing_returnsCount() {
        when(searchRepository.countPostsByHashtag("java")).thenReturn(5);

        Integer result = searchService.getHashtagCount("java");

        assertThat(result).isEqualTo(5);
    }

    @Test
    @DisplayName("getHashtagCount: returns 0 for non-existent tag")
    void getHashtagCount_nonExistent_returnsZero() {
        when(searchRepository.countPostsByHashtag("unknown")).thenReturn(null);

        Integer result = searchService.getHashtagCount("unknown");

        assertThat(result).isZero();
    }

    @Test
    @DisplayName("getHashtagCount: throws for blank tag")
    void getHashtagCount_blank_throws() {
        assertThatThrownBy(() -> searchService.getHashtagCount(""))
                .isInstanceOf(InvalidSearchQueryException.class);
    }
}
