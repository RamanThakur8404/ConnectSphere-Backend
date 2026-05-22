package com.connectsphere.media_service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.connectsphere.media_service.constants.MediaTypes;
import com.connectsphere.media_service.dto.MediaRequestDto;
import com.connectsphere.media_service.dto.MediaResponseDto;
import com.connectsphere.media_service.entity.Media;

@DisplayName("MediaMapper Tests")
class MediaMapperTest {

    private MediaMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new MediaMapper();
    }

    @Test
    void toEntity() {
        MediaRequestDto dto = MediaRequestDto.builder()
                .uploaderId(1).mediaTypes(MediaTypes.IMAGE)
                .sizeKb(100L).mimeType("image/jpeg").linkedPostId(10).build();

        Media entity = mapper.toEntity(dto);

        assertThat(entity.getUploaderId()).isEqualTo(1);
        assertThat(entity.getMediaTypes()).isEqualTo(MediaTypes.IMAGE);
        assertThat(entity.getIsDeleted()).isFalse();
    }

    @Test
    void toEntity_null() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void toResponseDto() {
        Media media = Media.builder()
                .mediaId(1).uploaderId(1).url("http://cdn/img.jpg")
                .mediaTypes(MediaTypes.IMAGE).sizeKb(100L)
                .mimeType("image/jpeg").linkedPostId(10).isDeleted(false).build();

        MediaResponseDto dto = mapper.toResponseDto(media);

        assertThat(dto.getMediaId()).isEqualTo(1);
        assertThat(dto.getUrl()).isEqualTo("http://cdn/img.jpg");
    }

    @Test
    void toResponseDto_null() {
        assertThat(mapper.toResponseDto(null)).isNull();
    }
}
