package com.connectsphere.post_service.service;

import java.util.List;
import java.util.Optional;

import com.connectsphere.post_service.constants.Visibility;
import com.connectsphere.post_service.dto.CreatePostRequest;
import com.connectsphere.post_service.dto.FeedRequest;
import com.connectsphere.post_service.dto.PostResponse;
import com.connectsphere.post_service.dto.SharePostRequest;
import com.connectsphere.post_service.dto.UpdatePostRequest;

// Service contract for all post-related business operations.
public interface PostService {

    // CRUD
    PostResponse createPost(CreatePostRequest request);
    Optional<PostResponse> getPostById(int postId);
    List<PostResponse> getPostsByUser(int authorId);
    PostResponse updatePost(int postId, UpdatePostRequest request);
    void deletePost(int postId);

    // Feed
    List<PostResponse> getPersonalisedFeed(FeedRequest feedRequest);
    List<PostResponse> getPublicFeed(Integer cursor, int limit);

    // Search
    List<PostResponse> searchPosts(String keyword);

    // Engagement counters
    void incrementLikes(int postId);
    void decrementLikes(int postId);
    void incrementComments(int postId);
    void decrementComments(int postId);
    void incrementShares(int postId);

    // Visibility
    void changeVisibility(int postId, Visibility visibility);

    // Stats
    int getPostCount(int authorId);

    // Share / Repost  (NEW)
    PostResponse sharePost(int originalPostId, SharePostRequest request);
    List<PostResponse> getSharesOfPost(int originalPostId);

    // Bookmark  (NEW)
    void bookmarkPost(int userId, int postId);
    void removeBookmark(int userId, int postId);
    List<PostResponse> getBookmarkedPosts(int userId);
}
