package com.connectsphere.search_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Event payload for post removal.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostRemoveEvent {

    // Post ID 
    private Integer postId;
}