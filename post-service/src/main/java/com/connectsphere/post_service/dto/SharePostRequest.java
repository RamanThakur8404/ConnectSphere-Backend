package com.connectsphere.post_service.dto;

import com.connectsphere.post_service.constants.Visibility;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO for sharing/reposting an existing post.
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Request payload for sharing/reposting a post")
public class SharePostRequest {

    @NotNull(message = "Sharing user ID is required")
    @Positive(message = "Author ID must be a positive integer")
    @Schema(description = "ID of the user who is sharing the post", example = "200", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer authorId;

    @Size(max = 500, message = "Share comment must not exceed 500 characters")
    @Schema(description = "Optional comment added by the user when sharing", example = "Great insight!")
    private String shareComment;

    @Schema(description = "Visibility for the new share post. Defaults to PUBLIC.", example = "PUBLIC")
    @Builder.Default
    private Visibility visibility = Visibility.PUBLIC;
}
