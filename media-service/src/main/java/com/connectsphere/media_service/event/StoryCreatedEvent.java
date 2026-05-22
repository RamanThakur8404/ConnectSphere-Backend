package com.connectsphere.media_service.event;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoryCreatedEvent {

    // ID of the newly created story. 
    private Integer storyId;

    // ID of the user who created the story. 
    private Integer authorId;

    // CDN URL of the story media. 
    private String mediaUrl;

    // IMAGE or VIDEO (string value of the enum). 
    private String mediaType;

    // Expiry timestamp — 24 hours after creation. 
    private LocalDateTime expiresAt;
}
