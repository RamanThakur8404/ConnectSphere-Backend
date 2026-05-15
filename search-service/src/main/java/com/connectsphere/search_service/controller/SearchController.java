package com.connectsphere.search_service.controller;

import com.connectsphere.search_service.dto.*;
import com.connectsphere.search_service.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// REST controller for the Search / Hashtag Service.
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@Slf4j
@Tag(name = "Search & Hashtags", description = "Post indexing, keyword search, and hashtag operations")
public class SearchController {

    private final SearchService searchService;

    @PostMapping("/search/index")
    @Operation(summary = "Index a post's hashtags")
    public ResponseEntity<ApiResponseDto<List<String>>> indexPost(
            @Valid @RequestBody IndexPostRequestDto request) {

        log.info("Received index request for post [postId={}]", request.getPostId());
        List<String> indexedTags = searchService.indexPost(
                request.getPostId(), request.getContent(), request.getHashtags());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDto.ok(
                        "Post indexed with " + indexedTags.size() + " hashtag(s)",
                        indexedTags));
    }

    @DeleteMapping("/search/index/{postId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Remove a post's hashtag index (admin only)")
    public ResponseEntity<ApiResponseDto<Void>> removePostIndex(
            @PathVariable @Positive(message = "Post ID must be positive") Integer postId) {

        log.info("Removing index for post [postId={}]", postId);
        searchService.removePostIndex(postId);
        return ResponseEntity.ok(ApiResponseDto.ok("Post index removed successfully", null));
    }

    @GetMapping("/search/posts")
    @Operation(summary = "Search posts by keyword or hashtag")
    public ResponseEntity<ApiResponseDto<PostIdListResponseDto>> searchPosts(
            @RequestParam
            @NotBlank(message = "Keyword must not be blank")
            @Parameter(description = "Search keyword (prefix with '#' for hashtag search)")
            String keyword) {

        log.debug("Search posts [keyword={}]", keyword);
        List<Integer> postIds = searchService.searchPosts(keyword);
        PostIdListResponseDto result = PostIdListResponseDto.builder()
                .postIds(postIds)
                .total(postIds.size())
                .build();
        return ResponseEntity.ok(ApiResponseDto.ok("Search completed", result));
    }

    @GetMapping("/search/users")
    @Operation(summary = "Search users by username or name fragment")
    public ResponseEntity<ApiResponseDto<UserIdListResponseDto>> searchUsers(
            @RequestParam
            @NotBlank(message = "Query must not be blank")
            @Parameter(description = "Partial username or full name to search")
            String query) {

        log.debug("Search users [query={}]", query);
        List<Integer> userIds = searchService.searchUsers(query);
        UserIdListResponseDto result = UserIdListResponseDto.builder()
                .userIds(userIds)
                .total(userIds.size())
                .build();
        return ResponseEntity.ok(ApiResponseDto.ok("User search completed", result));
    }

    @GetMapping("/hashtags/trending")
    @Operation(summary = "Get trending hashtags")
    public ResponseEntity<ApiResponseDto<List<HashtagSummaryDto>>> getTrendingHashtags() {
        log.debug("Fetching trending hashtags");
        List<HashtagSummaryDto> trending = searchService.getTrendingHashtags();
        return ResponseEntity.ok(ApiResponseDto.ok("Trending hashtags retrieved", trending));
    }

    @GetMapping("/hashtags")
    @Operation(summary = "Search / autocomplete hashtags")
    public ResponseEntity<ApiResponseDto<List<HashtagResponseDto>>> searchHashtags(
            @RequestParam(required = false, defaultValue = "")
            @Parameter(description = "Partial hashtag string for autocomplete")
            String fragment) {

        log.debug("Hashtag autocomplete [fragment={}]", fragment);
        List<HashtagResponseDto> results = searchService.searchHashtags(fragment);
        return ResponseEntity.ok(ApiResponseDto.ok("Hashtag search completed", results));
    }

    @GetMapping("/hashtags/{tag}/posts")
    @Operation(summary = "Get posts by hashtag")
    public ResponseEntity<ApiResponseDto<PostIdListResponseDto>> getPostsByHashtag(
            @PathVariable @Parameter(description = "Hashtag string") String tag) {

        log.debug("Fetching posts for hashtag [#{}]", tag);
        List<Integer> postIds = searchService.getPostsByHashtag(tag);
        PostIdListResponseDto result = PostIdListResponseDto.builder()
                .postIds(postIds)
                .total(postIds.size())
                .build();
        return ResponseEntity.ok(ApiResponseDto.ok("Posts retrieved for #" + tag, result));
    }

    @GetMapping("/hashtags/{tag}/count")
    @Operation(summary = "Get post count for a hashtag")
    public ResponseEntity<ApiResponseDto<Integer>> getHashtagCount(
            @PathVariable String tag) {

        log.debug("Getting post count for hashtag [#{}]", tag);
        Integer count = searchService.getHashtagCount(tag);
        return ResponseEntity.ok(ApiResponseDto.ok("Hashtag count retrieved", count));
    }

    @GetMapping("/posts/{postId}/hashtags")
    @Operation(summary = "Get hashtags for a post")
    public ResponseEntity<ApiResponseDto<List<HashtagResponseDto>>> getHashtagsForPost(
            @PathVariable @Positive(message = "Post ID must be positive") Integer postId) {

        log.debug("Fetching hashtags for post [postId={}]", postId);
        List<HashtagResponseDto> hashtags = searchService.getHashtagsForPost(postId);
        return ResponseEntity.ok(ApiResponseDto.ok("Hashtags retrieved for post " + postId, hashtags));
    }
}
