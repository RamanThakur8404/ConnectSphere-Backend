package com.connectsphere.media_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaUploadedEvent {

    // ID of the newly persisted media record. 
    private Integer mediaId;

    // ID of the user who uploaded the media. 
    private Integer uploaderId;

    // CDN URL where the file is hosted. 
    private String url;

    // IMAGE or VIDEO (string value of the enum). 
    private String mediaType;

    // Optional post ID this media is linked to. 
    private Integer linkedPostId;
}
