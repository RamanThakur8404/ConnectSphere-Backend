package com.connectsphere.like_service.mapper;

import org.springframework.stereotype.Component;

import com.connectsphere.like_service.dto.LikeRequestDTO;
import com.connectsphere.like_service.dto.LikeResponseDTO;
import com.connectsphere.like_service.entity.Like;

@Component
public class LikeMapper {
	public Like toEntity(LikeRequestDTO dto) {
        return Like.builder()
                .userId(dto.getUserId())
                .targetId(dto.getTargetId())
                .targetType(dto.getTargetType())
                .reactionType(dto.getReactionType())
                .build();
    }
	
	public LikeResponseDTO toResponseDTO(Like like) {
        return LikeResponseDTO.builder()
                .likeId(like.getLikeId())
                .userId(like.getUserId())
                .targetId(like.getTargetId())
                .targetType(like.getTargetType())
                .reactionType(like.getReactionType())
                .createdAt(like.getCreatedAt())
                .build();
    }
}
