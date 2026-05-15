package com.connectsphere.post_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

// Event message published to RabbitMQ when posts are created or deleted.
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PostEventMessage {

    public enum EventType { POST_CREATED, POST_DELETED, POST_UPDATED }

    private EventType eventType;
    private int postId;
    private int authorId;
    private String content;
    private List<String> mediaUrls;
    private List<String> hashtags;
    private String visibility;
    private String postType;
    private boolean contentWarning;
    private LocalDateTime createdAt;
}
