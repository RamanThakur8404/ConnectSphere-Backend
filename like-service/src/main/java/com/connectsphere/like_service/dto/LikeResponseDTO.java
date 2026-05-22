package com.connectsphere.like_service.dto;

import java.time.LocalDateTime;

import com.connectsphere.like_service.constant.ReactionType;
import com.connectsphere.like_service.constant.TargetType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LikeResponseDTO {
	private Integer likeId;
	private Integer userId;
	private Integer targetId;
	private TargetType targetType;
	private ReactionType reactionType;
	private LocalDateTime createdAt;
}
