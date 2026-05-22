package com.connectsphere.media_service.dto;

import com.connectsphere.media_service.constants.MediaTypes;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// DTO for creating a new Story. The binary media is uploaded first (via
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoryRequestDto {

	// ID of the authenticated user creating the story. 
	@NotNull(message = "Author ID must not be null")
	private Integer authorId;

	// CDN URL of the uploaded media file for this story. 
	@NotNull(message = "Media URL must not be null")
	private String mediaUrl;

	// Optional caption for the story. Maximum 500 characters.
	@Size(max = 500, message = "Caption must not exceed 500 characters")
	private String caption;

	// IMAGE or VIDEO — must match the actual uploaded file. 
	@NotNull(message = "Media type must not be null")
	private MediaTypes mediaTypes;
}
