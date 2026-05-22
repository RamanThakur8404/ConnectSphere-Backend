package com.connectsphere.post_service.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.connectsphere.post_service.constants.PostType;
import com.connectsphere.post_service.constants.Visibility;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO for creating a new post. Separates API contract from entity internals.
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Request payload for creating a new post")
public class CreatePostRequest {

    @NotNull(message = "Author ID is required")
    @Positive(message = "Author ID must be a positive integer")
    @Schema(description = "ID of the user creating the post", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer authorId;

    @NotBlank(message = "Post content must not be blank")
    @Size(min = 1, max = 5000, message = "Post content must be between 1 and 5000 characters")
    @Schema(description = "Text content of the post", example = "Hello ConnectSphere! #java #spring", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;

    @Size(max = 10, message = "Maximum 10 media URLs allowed per post")
    @Schema(description = "List of media URLs (images/videos) attached to the post")
    private List<String> mediaUrls;

    @Schema(description = "Post type: TEXT or MEDIA. Auto-resolved if omitted based on mediaUrls presence.")
    private PostType postType;

    @Schema(description = "Visibility: PUBLIC, FOLLOWERS_ONLY, or PRIVATE. Defaults to PUBLIC.", example = "PUBLIC")
    @Builder.Default
    private Visibility visibility = Visibility.PUBLIC;

    @Schema(description = "When true, the UI will overlay a content warning before showing the post.", example = "false")
    @Builder.Default
    private boolean contentWarning = false;

    @Schema(description = "If set, the post will not be publicly visible until this timestamp is reached.")
    private LocalDateTime scheduledPublishAt;
}
