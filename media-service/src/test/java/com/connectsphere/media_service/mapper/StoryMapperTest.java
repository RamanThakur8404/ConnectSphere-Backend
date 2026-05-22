package com.connectsphere.media_service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.connectsphere.media_service.constants.MediaTypes;
import com.connectsphere.media_service.dto.StoryRequestDto;
import com.connectsphere.media_service.dto.StoryResponseDto;
import com.connectsphere.media_service.entity.Story;

@DisplayName("StoryMapper Tests")
class StoryMapperTest {

    private StoryMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new StoryMapper();
    }

    @Test
    void toEntity() {
        StoryRequestDto dto = StoryRequestDto.builder()
                .authorId(1).mediaUrl("http://cdn/vid.mp4")
                .caption("Test").mediaTypes(MediaTypes.VIDEO).build();

        Story entity = mapper.toEntity(dto);

        assertThat(entity.getAuthorId()).isEqualTo(1);
        assertThat(entity.getMediaUrl()).isEqualTo("http://cdn/vid.mp4");
        assertThat(entity.getViewsCount()).isZero();
        assertThat(entity.getIsActive()).isTrue();
    }

    @Test
    void toEntity_null() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toResponseDto() {
        Story story = Story.builder()
                .storyId(1).authorId(1).mediaUrl("http://cdn/vid.mp4")
                .caption("Test").mediaTypes(MediaTypes.VIDEO)
                .viewsCount(5).isActive(true).build();

        StoryResponseDto dto = mapper.toResponseDto(story);

        assertThat(dto.getStoryId()).isEqualTo(1);
        assertThat(dto.getViewsCount()).isEqualTo(5);
        assertThat(dto.getIsActive()).isTrue();
    }

    @Test
    void toResponseDto_null() {
        assertThat(mapper.toResponseDto(null)).isNull();
    }
}
