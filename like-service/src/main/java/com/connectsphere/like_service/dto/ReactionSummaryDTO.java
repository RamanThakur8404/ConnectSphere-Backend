package com.connectsphere.like_service.dto;

import java.util.Map;

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
public class ReactionSummaryDTO {
	private Integer targetId;
	private TargetType targetType;
	private Integer totalCount;

	// Map of reactionType → count for each reaction present on this target 
	private Map<ReactionType, Long> reactionCounts;
}
