package com.connectsphere.like_service.service;

import com.connectsphere.like_service.constant.ReactionType;
import com.connectsphere.like_service.constant.TargetType;
import com.connectsphere.like_service.dto.*;
import com.connectsphere.like_service.entity.Like;

import java.util.List;
import java.util.Map;

public interface LikeService {

	LikeResponseDTO likeTarget(LikeRequestDTO request);

	void unlikeTarget(Integer userId, Integer targetId, TargetType targetType);

	boolean hasLiked(Integer userId, Integer targetId, TargetType targetType);

	Map<Integer, Boolean> hasLikedBatch(Integer userId, List<Integer> targetIds, TargetType targetType);

	List<LikeResponseDTO> getLikesByTarget(Integer targetId, TargetType targetType);

	List<LikeResponseDTO> getLikesByUser(Integer userId);

	int getLikeCount(Integer targetId, TargetType targetType);

	int getLikeCountByType(Integer targetId, TargetType targetType, ReactionType reactionType);

	ReactionSummaryDTO getReactionSummary(Integer targetId, TargetType targetType);

	LikeResponseDTO changeReaction(ChangeReactionDTO request);
}
