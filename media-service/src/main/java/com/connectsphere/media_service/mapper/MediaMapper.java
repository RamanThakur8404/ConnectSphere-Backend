package com.connectsphere.media_service.mapper;

import org.springframework.stereotype.Component;

import com.connectsphere.media_service.dto.MediaRequestDto;
import com.connectsphere.media_service.dto.MediaResponseDto;
import com.connectsphere.media_service.entity.Media;

// Mapper responsible for converting between {@link Media} entity and its DTOs.
@Component
public class MediaMapper {

    // Converts a {@link MediaRequestDto} to a {@link Media} entity.
    public Media toEntity(MediaRequestDto dto) {
        if (dto == null) {
            return null;
        }
        return Media.builder()
                .uploaderId(dto.getUploaderId())
                .url(dto.getUrl())
                .mediaTypes(dto.getMediaTypes())
                .sizeKb(dto.getSizeKb())
                .mimeType(dto.getMimeType())
                .linkedPostId(dto.getLinkedPostId())
                .isDeleted(false)
                .build();
    }

    // Converts a persisted {@link Media} entity to a {@link MediaResponseDto}.
    public MediaResponseDto toResponseDto(Media media) {
        if (media == null) {
            return null;
        }
        return MediaResponseDto.builder()
                .mediaId(media.getMediaId())
                .uploaderId(media.getUploaderId())
                .url(media.getUrl())
                .mediaTypes(media.getMediaTypes())
                .sizeKb(media.getSizeKb())
                .mimeType(media.getMimeType())
                .linkedPostId(media.getLinkedPostId())
                .uploadedAt(media.getUploadedAt())
                .isDeleted(media.getIsDeleted())
                .build();
    }
}
