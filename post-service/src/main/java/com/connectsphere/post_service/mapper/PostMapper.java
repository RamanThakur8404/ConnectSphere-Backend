package com.connectsphere.post_service.mapper;

import com.connectsphere.post_service.constants.PostType;
import com.connectsphere.post_service.constants.Visibility;
import com.connectsphere.post_service.dto.CreatePostRequest;
import com.connectsphere.post_service.dto.PostResponse;
import com.connectsphere.post_service.dto.UpdatePostRequest;
import com.connectsphere.post_service.entity.Post;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


// Stateless mapper between Post entity and its DTOs.
@Component
public class PostMapper {

    // Convert CreatePostRequest → Post entity.
    public Post toEntity(CreatePostRequest request) {
        PostType resolved = request.getPostType();
        if (resolved == null) {
            resolved = (request.getMediaUrls() != null && !request.getMediaUrls().isEmpty())
                    ? PostType.MEDIA
                    : PostType.TEXT;
        }

        Visibility vis = request.getVisibility() != null ? request.getVisibility() : Visibility.PUBLIC;

        return Post.builder()
                .authorId(request.getAuthorId())
                .content(request.getContent().trim())
                .mediaUrls(request.getMediaUrls() != null ? new ArrayList<>(request.getMediaUrls()) : new ArrayList<>())
                .hashtags(new ArrayList<>())
                .postType(resolved)
                .visibility(vis)
                .contentWarning(request.isContentWarning())
                .scheduledPublishAt(request.getScheduledPublishAt())
                .build();
    }

    // Apply UpdatePostRequest fields to an existing Post entity.
    public void applyUpdate(UpdatePostRequest request, Post post) {
        if (request.getContent() != null && !request.getContent().isBlank()) {
            post.setContent(request.getContent().trim());
        }
        if (request.getMediaUrls() != null) {
            post.setMediaUrls(new ArrayList<>(request.getMediaUrls()));
        }
        if (request.getPostType() != null) {
            post.setPostType(request.getPostType());
        }
        if (request.getVisibility() != null) {
            post.setVisibility(request.getVisibility());
        }
        post.setContentWarning(request.isContentWarning());
    }

    // Convert Post entity → PostResponse DTO.
    public PostResponse toResponse(Post post) {
        return PostResponse.builder()
                .postId(post.getPostId())
                .authorId(post.getAuthorId())
                .content(post.getContent())
                .mediaUrls(post.getMediaUrls() != null ? post.getMediaUrls() : Collections.emptyList())
                .hashtags(post.getHashtags() != null ? post.getHashtags() : Collections.emptyList())
                .postType(post.getPostType())
                .visibility(post.getVisibility())
                .likesCount(post.getLikesCount())
                .commentsCount(post.getCommentsCount())
                .sharesCount(post.getSharesCount())
                .originalPostId(post.getOriginalPostId())
                .contentWarning(post.isContentWarning())
                .scheduledPublishAt(post.getScheduledPublishAt())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    // Convert a list of Post entities → list of PostResponse DTOs.
    public List<PostResponse> toResponseList(List<Post> posts) {
        if (posts == null || posts.isEmpty()) return Collections.emptyList();
        return posts.stream().map(this::toResponse).toList();
    }
}
