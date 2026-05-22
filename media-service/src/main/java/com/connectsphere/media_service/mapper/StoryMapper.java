package com.connectsphere.media_service.mapper;


import org.springframework.stereotype.Component;

import com.connectsphere.media_service.dto.StoryRequestDto;
import com.connectsphere.media_service.dto.StoryResponseDto;
import com.connectsphere.media_service.entity.Story;

// Mapper responsible for converting between {@link Story} entity and its DTOs.
@Component
public class StoryMapper {

    // Converts a {@link StoryRequestDto} to a {@link Story} entity.
    public Story toEntity(StoryRequestDto dto) {
        if (dto == null) {
            return null;
        }
        return Story.builder()
                .authorId(dto.getAuthorId())
                .mediaUrl(dto.getMediaUrl())
                .caption(dto.getCaption())
                .mediaTypes(dto.getMediaTypes())
                .viewsCount(0)
                .isActive(true)
                .build();
    }

    // Converts a persisted {@link Story} entity to a {@link StoryResponseDto}.
    public StoryResponseDto toResponseDto(Story story) {
        if (story == null) {
            return null;
        }
        return StoryResponseDto.builder()
                .storyId(story.getStoryId())
                .authorId(story.getAuthorId())
                .mediaUrl(story.getMediaUrl())
                .caption(story.getCaption())
                .mediaTypes(story.getMediaTypes())
                .viewsCount(story.getViewsCount())
                .expiresAt(story.getExpiresAt())
                .createdAt(story.getCreatedAt())
                .isActive(story.getIsActive())
                .build();
    }
}
