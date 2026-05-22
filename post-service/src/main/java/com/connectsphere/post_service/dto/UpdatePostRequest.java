package com.connectsphere.post_service.dto;

import java.util.List;

import com.connectsphere.post_service.constants.PostType;
import com.connectsphere.post_service.constants.Visibility;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO for updating an existing post's content, media, type, or visibility.
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Request payload for updating an existing post")
public class UpdatePostRequest {

    @NotBlank(message = "Post content must not be blank")
    @Size(min = 1, max = 5000, message = "Post content must be between 1 and 5000 characters")
    @Schema(description = "Updated text content of the post")
    private String content;

    @Size(max = 10, message = "Maximum 10 media URLs allowed per post")
    @Schema(description = "Updated list of media URLs")
    private List<String> mediaUrls;

    @Schema(description = "Updated post type")
    private PostType postType;

    @Schema(description = "Updated visibility setting")
    private Visibility visibility;

    @Schema(description = "Updated content warning flag")
    private boolean contentWarning;
}
