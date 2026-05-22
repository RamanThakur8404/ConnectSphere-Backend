package com.connectsphere.post_service.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO for requesting a personalised feed with cursor-based pagination.
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Request payload for fetching the personalised news feed")
public class FeedRequest {

    @NotEmpty(message = "Followee IDs list must not be empty")
    @Schema(description = "Followed user IDs. Feed includes all PUBLIC posts and FOLLOWERS_ONLY posts from these users.", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Integer> followeeIds;

    @Schema(description = "Cursor for pagination — the postId to start after (exclusive). Omit for first page.")
    private Integer cursor;

    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 50, message = "Limit must not exceed 50")
    @Builder.Default
    @Schema(description = "Number of posts to return per page (1–50). Defaults to 20.", example = "20")
    private int limit = 20;
}
