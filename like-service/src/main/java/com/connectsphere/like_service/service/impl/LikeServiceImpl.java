package com.connectsphere.like_service.service.impl;

import com.connectsphere.like_service.client.CommentServiceClient;
import com.connectsphere.like_service.client.PostServiceClient;
import com.connectsphere.like_service.constant.ErrorMessages;
import com.connectsphere.like_service.constant.LogMessages;
import com.connectsphere.like_service.constant.ReactionType;
import com.connectsphere.like_service.constant.TargetType;
import com.connectsphere.like_service.dto.*;
import com.connectsphere.like_service.entity.Like;
import com.connectsphere.like_service.exception.DuplicateLikeException;
import com.connectsphere.like_service.exception.InvalidTargetTypeException;
import com.connectsphere.like_service.exception.LikeNotFoundException;
import com.connectsphere.like_service.mapper.LikeMapper;
import com.connectsphere.like_service.repository.LikeRepository;
import com.connectsphere.like_service.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

	private static final Logger logger = LoggerFactory.getLogger(LikeServiceImpl.class);

	private final LikeRepository likeRepository;
	private final LikeMapper likeMapper;
	private final PostServiceClient postServiceClient;
	private final CommentServiceClient commentServiceClient;

	@Override
	@Transactional
	public LikeResponseDTO likeTarget(LikeRequestDTO request) {
		logger.info("User {} reacting to {} {} with {}", request.getUserId(), request.getTargetType(),
				request.getTargetId(), request.getReactionType());

		boolean alreadyLiked = likeRepository.existsByUserIdAndTargetId(request.getUserId(), request.getTargetId());

		if (alreadyLiked) {
			logger.warn(LogMessages.LIKE_DUPLICATE, request.getUserId(), request.getTargetType(),
					request.getTargetId());
			throw new DuplicateLikeException(
                    ErrorMessages.DUPLICATE_REACTION + request.getUserId()
                    + ErrorMessages.ALREADY_REACTED + request.getTargetType()
                    + " " + request.getTargetId());		}

		Like like = likeMapper.toEntity(request);
		Like saved = likeRepository.save(like);

		// Feign call: increment like count on the target entity
		try {
			if (request.getTargetType() == TargetType.POST) {
				postServiceClient.incrementLikeCount(request.getTargetId());
			} else if (request.getTargetType() == TargetType.COMMENT) {
				commentServiceClient.incrementLikeCount(request.getTargetId().longValue());
			}
			logger.info(LogMessages.LIKE_COUNT_INCREMENT_OK, request.getTargetType(), request.getTargetId());
		} catch (Exception e) {
			logger.warn(LogMessages.LIKE_COUNT_INCREMENT_ERR, request.getTargetType(), e.getMessage());
		}

		logger.info(LogMessages.LIKE_SUCCESS, saved.getLikeId());
		return likeMapper.toResponseDTO(saved);
	}

	@Override
	@Transactional
	public void unlikeTarget(Integer userId, Integer targetId, TargetType targetType) {
		logger.info(LogMessages.UNLIKE_ATTEMPT, userId, targetType, targetId);
		if (!likeRepository.existsByUserIdAndTargetId(userId, targetId)) {
			logger.warn(LogMessages.UNLIKE_NOT_FOUND, userId, targetType, targetId);
			throw new LikeNotFoundException(
					ErrorMessages.REACTION_NOT_FOUND + userId + ErrorMessages.REACTION_NOT_FOUND_ON + targetType + " " + targetId);
		}

		likeRepository.deleteByUserIdAndTargetId(userId, targetId);

		// Feign call: decrement like count on the target entity
		try {
			if (targetType == TargetType.POST) {
				postServiceClient.decrementLikeCount(targetId);
			} else if (targetType == TargetType.COMMENT) {
				commentServiceClient.decrementLikeCount(targetId.longValue());
			}
			logger.info(LogMessages.LIKE_COUNT_DECREMENT_OK, targetType, targetId);
		} catch (Exception e) {
			logger.warn(LogMessages.LIKE_COUNT_DECREMENT_ERR, targetType, e.getMessage());
		}

		logger.info(LogMessages.UNLIKE_SUCCESS, userId, targetType, targetId);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean hasLiked(Integer userId, Integer targetId, TargetType targetType) {
		logger.debug(LogMessages.HAS_LIKED_CHECK, userId, targetType, targetId);
		return likeRepository.existsByUserIdAndTargetId(userId, targetId);
	}

	@Override
	@Transactional(readOnly = true)
	public Map<Integer, Boolean> hasLikedBatch(Integer userId, List<Integer> targetIds, TargetType targetType) {
		logger.debug("Checking hasLiked batch for user {} on targets {}", userId, targetIds);
		List<Like> likes = likeRepository.findByUserIdAndTargetIdInAndTargetType(userId, targetIds, targetType);
		
		Map<Integer, Boolean> resultMap = new HashMap<>();
		for (Integer targetId : targetIds) {
			resultMap.put(targetId, false);
		}
		for (Like like : likes) {
			resultMap.put(like.getTargetId(), true);
		}
		return resultMap;
	}

	@Override
	@Transactional(readOnly = true)
	public List<LikeResponseDTO> getLikesByTarget(Integer targetId, TargetType targetType) {
		logger.debug(LogMessages.GET_LIKES_BY_TARGET, targetType, targetId);
		return likeRepository.findByTargetId(targetId).stream().map(likeMapper::toResponseDTO)
				.collect(Collectors.toList());
	}

	@Override
	@Transactional(readOnly = true)
	public List<LikeResponseDTO> getLikesByUser(Integer userId) {
		logger.debug(LogMessages.GET_LIKES_BY_USER, userId);
		return likeRepository.findByUserId(userId).stream().map(likeMapper::toResponseDTO).collect(Collectors.toList());
	}

	//
	@Override
	@Transactional(readOnly = true)
	public int getLikeCount(Integer targetId, TargetType targetType) {
		logger.debug(LogMessages.GET_LIKE_COUNT, targetType, targetId);
		return likeRepository.countByTargetId(targetId);
	}

	//
	@Override
	@Transactional(readOnly = true)
	public int getLikeCountByType(Integer targetId, TargetType targetType, ReactionType reactionType) {
		logger.debug(LogMessages.GET_LIKE_COUNT_BY_TYPE, reactionType, targetType, targetId);
		return likeRepository.countByTargetId(targetId);
	}

	@Override
	@Transactional(readOnly = true)
	public ReactionSummaryDTO getReactionSummary(Integer targetId, TargetType targetType) {
		logger.debug(LogMessages.GET_REACTION_SUMMARY, targetType, targetId);

		List<Object[]> rawSummary = likeRepository.findReactionSummary(targetId);
		Map<ReactionType, Long> reactionCounts = new HashMap<>();

		for (Object[] row : rawSummary) {
			ReactionType reactionType = (ReactionType) row[0];
			Long count = (Long) row[1];
			reactionCounts.put(reactionType, count);
		}

		int total = reactionCounts.values().stream().mapToInt(Long::intValue).sum();

		return ReactionSummaryDTO.builder().targetId(targetId).totalCount(total)
				.reactionCounts(reactionCounts).build();
	}

	// field.
	@Override
	@Transactional
	public LikeResponseDTO changeReaction(ChangeReactionDTO request) {
		logger.info(LogMessages.CHANGE_REACTION_ATTEMPT, request.getUserId(), request.getTargetType(),
				request.getTargetId(), request.getNewReactionType());
		Like existing = likeRepository
				.findByUserIdAndTargetId(request.getUserId(), request.getTargetId())
				.orElseThrow(() -> new LikeNotFoundException(ErrorMessages.REACTION_NOT_FOUND
						+ request.getUserId() + ErrorMessages.REACTION_NOT_FOUND_ON + request.getTargetType() + " " + request.getTargetId()));

		existing.setReactionType(request.getNewReactionType());
		Like updated = likeRepository.save(existing);

		logger.info(LogMessages.CHANGE_REACTION_SUCCESS, updated.getLikeId(), updated.getReactionType());
		return likeMapper.toResponseDTO(updated);
	}
}
