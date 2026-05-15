package com.connectsphere.media_service.dto;

import com.connectsphere.media_service.constants.MediaTypes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// DTO carrying the metadata for a new media upload request. The binary file
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaRequestDto {

	// ID of the authenticated user performing the upload. 
	@NotNull(message = "Uploader ID must not be null")
	private Integer uploaderId;

	// CDN / S3 URL populated by the service after the file is stored. Not expected
	private String url;

	// IMAGE or VIDEO. 
	@NotNull(message = "Media type must not be null")
	private MediaTypes mediaTypes;

	// File size in kilobytes — derived from the uploaded MultipartFile. 
	@Positive(message = "File size must be positive")
	@NotNull(message = "File size must not be null")
	private Long sizeKb;

	// MIME type, e.g. image/jpeg, video/mp4. 
	@NotBlank(message = "MIME type must not be blank")
	private String mimeType;

	// ID of the post to link this media to. Optional — can be linked later via
	private Integer linkedPostId;
}
