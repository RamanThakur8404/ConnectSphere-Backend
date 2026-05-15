package com.connectsphere.search_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

// Request DTO sent by post-service to index a post's hashtags.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndexPostRequestDto {

    @NotNull(message = "Post ID must not be null")
    @Positive(message = "Post ID must be a positive integer")
    private Integer postId;

    @NotBlank(message = "Post content must not be blank")
    @Size(max = 5000, message = "Post content must not exceed 5000 characters")
    private String content;

    @Builder.Default
    private List<String> hashtags = List.of();
}
