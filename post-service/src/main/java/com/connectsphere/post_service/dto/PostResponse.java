package com.connectsphere.post_service.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.connectsphere.post_service.constants.PostType;
import com.connectsphere.post_service.constants.Visibility;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO returned for all post read operations.
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Post response payload")
public class PostResponse {

    @Schema(description = "Unique post identifier", example = "1")
    private int postId;

    @Schema(description = "ID of the author", example = "100")
    private int authorId;

    @Schema(description = "Text content of the post")
    private String content;

    @Schema(description = "List of attached media URLs")
    private List<String> mediaUrls;

    @Schema(description = "Hashtags attached to this post, without the # symbol")
    private List<String> hashtags;

    @Schema(description = "Post type: TEXT or MEDIA", example = "TEXT")
    private PostType postType;

    @Schema(description = "Visibility: PUBLIC, FOLLOWERS_ONLY, or PRIVATE", example = "PUBLIC")
    private Visibility visibility;

    @Schema(description = "Number of likes", example = "42")
    private int likesCount;

    @Schema(description = "Number of comments", example = "7")
    private int commentsCount;

    @Schema(description = "Number of shares/reposts", example = "3")
    private int sharesCount;

    @Schema(description = "For reshares: ID of the original post. Null for original posts.")
    private Integer originalPostId;

    @Schema(description = "Whether this post has a content warning overlay", example = "false")
    private boolean contentWarning;

    @Schema(description = "Scheduled publish time. Null if published immediately.")
    private LocalDateTime scheduledPublishAt;

    @Schema(description = "Timestamp of post creation")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp of last update")
    private LocalDateTime updatedAt;
}
