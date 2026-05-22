package com.connectsphere.media_service.dto;


import java.time.LocalDateTime;

import com.connectsphere.media_service.constants.MediaTypes;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// DTO returned to API consumers after a successful media upload or retrieval.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaResponseDto {

    // Unique identifier of the media record. 
    private Integer mediaId;

    // ID of the uploader. 
    private Integer uploaderId;

    // Publicly accessible CDN URL for the media file. 
    private String url;

    // IMAGE or VIDEO. 
    private MediaTypes mediaTypes;

    // File size in kilobytes. 
    private Long sizeKb;

    // MIME type of the file. 
    private String mimeType;

    // ID of the linked post, if any. 
    private Integer linkedPostId;

    // Timestamp when the file was uploaded. 
    private LocalDateTime uploadedAt;

    // Indicates if this media has been soft-deleted. 
    private Boolean isDeleted;
}
