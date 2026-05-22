package com.connectsphere.search_service.service;

import com.connectsphere.search_service.dto.*;

import java.util.List;

// Business contract for the Search / Hashtag Service.
public interface SearchService {

    List<String> indexPost(Integer postId, String content, List<String> hashtags);

    void removePostIndex(Integer postId);

    List<Integer> searchPosts(String keyword);

    List<Integer> searchUsers(String query);

    List<HashtagResponseDto> getHashtagsForPost(Integer postId);

    List<HashtagSummaryDto> getTrendingHashtags();

    List<Integer> getPostsByHashtag(String tag);

    List<HashtagResponseDto> searchHashtags(String fragment);

    Integer getHashtagCount(String tag);
}
