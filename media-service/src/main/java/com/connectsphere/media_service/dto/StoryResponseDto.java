package com.connectsphere.media_service.dto;

import java.time.LocalDateTime;
import com.connectsphere.media_service.constants.MediaTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// DTO returned to clients after story creation or retrieval. Includes expiry
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoryResponseDto {

	// Unique identifier of the story. 
	private Integer storyId;

	// ID of the story author. 
	private Integer authorId;

	// CDN URL of the story media. 
	private String mediaUrl;

	// Optional caption text. 
	private String caption;

	// IMAGE or VIDEO. 
	private MediaTypes mediaTypes;

	// Total number of views recorded for this story. 
	private Integer viewsCount;

	// Exact timestamp at which this story expires (createdAt + 24h). 
	private LocalDateTime expiresAt;

	// Timestamp when the story was created. 
	private LocalDateTime createdAt;

	// Whether the story is still active (not expired or deleted). 
	private Boolean isActive;
}
