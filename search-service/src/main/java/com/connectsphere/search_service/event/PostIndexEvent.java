package com.connectsphere.search_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// Event payload for post index (create/update).
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostIndexEvent {

    // Post ID 
    private Integer postId;

    // Post content (used for hashtag extraction) 
    private String content;

    // Backend-generated or explicit tags attached by post-service.
    private List<String> hashtags;
}
