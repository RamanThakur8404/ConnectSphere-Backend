package com.connectsphere.like_service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.connectsphere.like_service.constant.ReactionType;
import com.connectsphere.like_service.constant.TargetType;
import com.connectsphere.like_service.dto.LikeRequestDTO;
import com.connectsphere.like_service.dto.LikeResponseDTO;
import com.connectsphere.like_service.entity.Like;

@DisplayName("LikeMapper Unit Tests")
class LikeMapperTest {

    private LikeMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new LikeMapper();
    }

    @Test
    @DisplayName("toEntity maps all fields correctly")
    void toEntity() {
        LikeRequestDTO dto = new LikeRequestDTO();
        dto.setUserId(100);
        dto.setTargetId(10);
        dto.setTargetType(TargetType.POST);
        dto.setReactionType(ReactionType.LIKE);

        Like entity = mapper.toEntity(dto);

        assertThat(entity.getUserId()).isEqualTo(100);
        assertThat(entity.getTargetId()).isEqualTo(10);
        assertThat(entity.getTargetType()).isEqualTo(TargetType.POST);
        assertThat(entity.getReactionType()).isEqualTo(ReactionType.LIKE);
    }

    @Test
    @DisplayName("toResponseDTO maps all fields correctly")
    void toResponseDTO() {
        LocalDateTime now = LocalDateTime.now();
        Like like = Like.builder()
                .likeId(1)
                .userId(100)
                .targetId(10)
                .targetType(TargetType.COMMENT)
                .reactionType(ReactionType.LOVE)
                .createdAt(now)
                .build();

        LikeResponseDTO dto = mapper.toResponseDTO(like);

        assertThat(dto.getLikeId()).isEqualTo(1);
        assertThat(dto.getUserId()).isEqualTo(100);
        assertThat(dto.getTargetId()).isEqualTo(10);
        assertThat(dto.getTargetType()).isEqualTo(TargetType.COMMENT);
        assertThat(dto.getReactionType()).isEqualTo(ReactionType.LOVE);
        assertThat(dto.getCreatedAt()).isEqualTo(now);
    }
}
