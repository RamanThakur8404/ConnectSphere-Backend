package com.connectsphere.search_service.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.connectsphere.search_service.dto.HashtagResponseDto;
import com.connectsphere.search_service.dto.HashtagSummaryDto;
import com.connectsphere.search_service.dto.IndexPostRequestDto;
import com.connectsphere.search_service.exception.HashtagNotFoundException;
import com.connectsphere.search_service.security.GatewayHeaderAuthFilter;
import com.connectsphere.search_service.service.SearchService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(SearchController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SearchResource Controller Tests")
class SearchControllerTest {

    private static final String BASE = "/api/v1";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchService searchService;

    @MockBean
    private GatewayHeaderAuthFilter gatewayHeaderAuthFilter;

    @Autowired
    private ObjectMapper objectMapper;

    // ═════════════════════════════════════════════════════════════════════════
    // POST /search/index
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("POST /search/index")
    class IndexPostTests {

        @Test
        @DisplayName("Should return 201 with indexed tags on valid request")
        void indexPost_valid_returns201() throws Exception {
            when(searchService.indexPost(eq(1), anyString(), anyList()))
                    .thenReturn(List.of("java", "spring"));

            IndexPostRequestDto request = IndexPostRequestDto.builder()
                    .postId(1)
                    .content("Learning #Java and #Spring")
                    .hashtags(List.of("java", "spring"))
                    .build();

            mockMvc.perform(post(BASE + "/search/index")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0]").value("java"));
        }

        @Test
        @DisplayName("Should return 400 when postId is missing")
        void indexPost_missingPostId_returns400() throws Exception {
            IndexPostRequestDto request = IndexPostRequestDto.builder()
                    .content("Some content")
                    .build();

            mockMvc.perform(post(BASE + "/search/index")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when content is blank")
        void indexPost_blankContent_returns400() throws Exception {
            IndexPostRequestDto request = IndexPostRequestDto.builder()
                    .postId(1)
                    .content("")
                    .build();

            mockMvc.perform(post(BASE + "/search/index")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GET /search/posts
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /search/posts")
    class SearchPostsTests {

        @Test
        @DisplayName("Should return 200 with post IDs for valid keyword")
        void searchPosts_valid_returns200() throws Exception {
            when(searchService.searchPosts("java")).thenReturn(List.of(10, 20, 30));

            mockMvc.perform(get(BASE + "/search/posts")
                            .param("keyword", "java"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.postIds", hasSize(3)))
                    .andExpect(jsonPath("$.data.total").value(3));
        }

        @Test
        @DisplayName("Should return 200 with empty list when no matches")
        void searchPosts_noMatch_returnsEmpty() throws Exception {
            when(searchService.searchPosts("python")).thenReturn(Collections.emptyList());

            mockMvc.perform(get(BASE + "/search/posts")
                            .param("keyword", "python"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.postIds", hasSize(0)));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GET /search/users
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /search/users")
    class SearchUsersTests {

        @Test
        @DisplayName("Should return 200 with user IDs for valid query")
        void searchUsers_valid_returns200() throws Exception {
            when(searchService.searchUsers("john")).thenReturn(List.of(1, 2));

            mockMvc.perform(get(BASE + "/search/users")
                            .param("query", "john"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.userIds", hasSize(2)));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GET /hashtags/trending
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /hashtags/trending")
    class TrendingTests {

        @Test
        @DisplayName("Should return 200 with trending hashtags")
        void getTrending_returns200() throws Exception {
            HashtagSummaryDto summary = HashtagSummaryDto.builder()
                    .tag("java")
                    .postCount(100)
                    .lastUsedAt(LocalDateTime.now())
                    .build();
            when(searchService.getTrendingHashtags()).thenReturn(List.of(summary));

            mockMvc.perform(get(BASE + "/hashtags/trending"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].tag").value("java"))
                    .andExpect(jsonPath("$.data[0].postCount").value(100));
        }

        @Test
        @DisplayName("Should return 200 with empty list when nothing is trending")
        void getTrending_empty_returns200() throws Exception {
            when(searchService.getTrendingHashtags()).thenReturn(Collections.emptyList());

            mockMvc.perform(get(BASE + "/hashtags/trending"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GET /hashtags
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /hashtags")
    class SearchHashtagsTests {

        @Test
        @DisplayName("Should return 200 with matching hashtags")
        void searchHashtags_withFragment_returns200() throws Exception {
            HashtagResponseDto dto = HashtagResponseDto.builder()
                    .hashtagId(1)
                    .tag("java")
                    .postCount(5)
                    .build();
            when(searchService.searchHashtags("jav")).thenReturn(List.of(dto));

            mockMvc.perform(get(BASE + "/hashtags")
                            .param("fragment", "jav"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].tag").value("java"));
        }

        @Test
        @DisplayName("Should return 200 with empty list when no fragment matches")
        void searchHashtags_noFragment_returns200() throws Exception {
            when(searchService.searchHashtags("")).thenReturn(Collections.emptyList());

            mockMvc.perform(get(BASE + "/hashtags"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GET /hashtags/{tag}/posts
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /hashtags/{tag}/posts")
    class GetPostsByHashtagTests {

        @Test
        @DisplayName("Should return 200 with post IDs for existing tag")
        void getPostsByHashtag_exists_returns200() throws Exception {
            when(searchService.getPostsByHashtag("java")).thenReturn(List.of(10, 20));

            mockMvc.perform(get(BASE + "/hashtags/java/posts"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.postIds", hasSize(2)));
        }

        @Test
        @DisplayName("Should return 404 when tag not found")
        void getPostsByHashtag_notFound_returns404() throws Exception {
            when(searchService.getPostsByHashtag("unknown"))
                    .thenThrow(new HashtagNotFoundException("Hashtag not found: #unknown"));

            mockMvc.perform(get(BASE + "/hashtags/unknown/posts"))
                    .andExpect(status().isNotFound());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GET /hashtags/{tag}/count
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /hashtags/{tag}/count")
    class GetHashtagCountTests {

        @Test
        @DisplayName("Should return 200 with post count")
        void getCount_existing_returns200() throws Exception {
            when(searchService.getHashtagCount("java")).thenReturn(42);

            mockMvc.perform(get(BASE + "/hashtags/java/count"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(42));
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GET /posts/{postId}/hashtags
    // ═════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("GET /posts/{postId}/hashtags")
    class GetHashtagsForPostTests {

        @Test
        @DisplayName("Should return 200 with hashtags for a post")
        void getHashtagsForPost_existing_returns200() throws Exception {
            HashtagResponseDto dto = HashtagResponseDto.builder()
                    .hashtagId(1)
                    .tag("java")
                    .postCount(5)
                    .build();
            when(searchService.getHashtagsForPost(10)).thenReturn(List.of(dto));

            mockMvc.perform(get(BASE + "/posts/10/hashtags"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].tag").value("java"));
        }

        @Test
        @DisplayName("Should return 200 with empty list for post with no hashtags")
        void getHashtagsForPost_noHashtags_returnsEmpty() throws Exception {
            when(searchService.getHashtagsForPost(999)).thenReturn(Collections.emptyList());

            mockMvc.perform(get(BASE + "/posts/999/hashtags"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }
    }
}
