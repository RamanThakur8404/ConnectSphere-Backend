package com.connectsphere.media_service.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.connectsphere.media_service.constants.MediaTypes;
import com.connectsphere.media_service.dto.MediaRequestDto;
import com.connectsphere.media_service.dto.MediaResponseDto;
import com.connectsphere.media_service.dto.StoryRequestDto;
import com.connectsphere.media_service.dto.StoryResponseDto;
import com.connectsphere.media_service.entity.Media;
import com.connectsphere.media_service.entity.Story;
import com.connectsphere.media_service.exception.InvalidMediaTypeException;
import com.connectsphere.media_service.exception.MediaNotFoundException;
import com.connectsphere.media_service.exception.StoryNotFoundException;
import com.connectsphere.media_service.mapper.MediaMapper;
import com.connectsphere.media_service.mapper.StoryMapper;
import com.connectsphere.media_service.repository.MediaRepository;
import com.connectsphere.media_service.repository.StoryRepository;
import com.connectsphere.media_service.service.MediaService;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

	private static final Logger logger = LoggerFactory.getLogger(MediaServiceImpl.class);

	// Allowed MIME types as specified in section 2.6 of the case study 
	private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "video/mp4");

	private final MediaRepository mediaRepository;
	private final StoryRepository storyRepository;
	private final MediaMapper mediaMapper;
	private final StoryMapper storyMapper;

	// Media operations

	// Validates the MIME type before saving. Throws
	@Override
	public MediaResponseDto uploadMedia(MediaRequestDto dto, String cdnUrl) {
		logger.info("Uploading media for uploaderId={}, mimeType={}", dto.getUploaderId(), dto.getMimeType());

		// Validate MIME type
		validateMimeType(dto.getMimeType());

		// Map DTO to entity and attach the CDN URL returned by the storage layer
		Media media = mediaMapper.toEntity(dto);
		media.setUrl(cdnUrl);

		Media saved = mediaRepository.save(media);
		logger.info("Media persisted with mediaId={}", saved.getMediaId());

		return mediaMapper.toResponseDto(saved);
	}

	//
	@Override
	@Transactional(readOnly = true)
	public List<MediaResponseDto> getMediaByPost(int postId) {
		logger.debug("Fetching media for postId={}", postId);
		return mediaRepository.findByLinkedPostId(postId).stream().map(mediaMapper::toResponseDto)
				.collect(Collectors.toList());
	}

	// soft-deleted
	@Override
	@Transactional(readOnly = true)
	public MediaResponseDto getMediaById(int mediaId) {
		logger.debug("Fetching media with mediaId={}", mediaId);
		Media media = mediaRepository.findByMediaId(mediaId).orElseThrow(() -> new MediaNotFoundException(mediaId));
		return mediaMapper.toResponseDto(media);
	}

	// Performs a soft-delete: sets isDeleted = true. The physical row and CDN file
	@Override
	public void deleteMedia(int mediaId) {
		logger.info("Soft-deleting media with mediaId={}", mediaId);
		// Verify existence before updating
		Media media = mediaRepository.findByMediaId(mediaId).orElseThrow(() -> new MediaNotFoundException(mediaId));
		mediaRepository.deleteByMediaId(media.getMediaId());
		logger.info("Media mediaId={} soft-deleted", mediaId);
	}

	// Story operations

	// The expiresAt field is automatically computed as createdAt + 24 hours by the
	@Override
	public StoryResponseDto createStory(StoryRequestDto dto) {
		logger.info("Creating story for authorId={}", dto.getAuthorId());

		// 2. Validate format logically if possible
		validateMimeType(dto.getMediaTypes() == MediaTypes.IMAGE ? "image/jpeg" : "video/mp4");

		Story story = storyMapper.toEntity(dto);
		Story saved = storyRepository.save(story);
		logger.info("Story created with storyId={}, expiresAt={}", saved.getStoryId(), saved.getExpiresAt());

		return storyMapper.toResponseDto(saved);
	}

	//
	@Override
	@Transactional(readOnly = true)
	public List<StoryResponseDto> getActiveStories(List<Integer> authorIds) {
		if (authorIds == null || authorIds.isEmpty()) {
			logger.debug("No authorIds provided for active stories lookup");
			return List.of();
		}

		logger.debug("Fetching active stories for {} authors", authorIds.size());
		return storyRepository.findActiveStoriesByAuthorIds(authorIds).stream().map(storyMapper::toResponseDto)
				.collect(Collectors.toList());
	}

	// View count is incremented atomically via a single UPDATE query to avoid
	@Override
	public void viewStory(int storyId, int viewerId) {
		logger.info("User viewerId={} viewed storyId={}", viewerId, storyId);

		// Confirm story is still active before incrementing
		Story story = storyRepository.findActiveById(storyId).orElseThrow(() -> new StoryNotFoundException(storyId));

		storyRepository.incrementViewCount(story.getStoryId());
	}

	// Sets isActive = false. The row is kept for potential future use.
	@Override
	public void deleteStory(int storyId) {
		logger.info("Soft-deleting storyId={}", storyId);
		Story story = storyRepository.findActiveById(storyId).orElseThrow(() -> new StoryNotFoundException(storyId));
		storyRepository.deactivateStory(story.getStoryId());
		logger.info("Story storyId={} deactivated", storyId);
	}

	//
	@Override
	@Transactional(readOnly = true)
	public List<StoryResponseDto> getStoriesByUser(int authorId) {
		logger.debug("Fetching active stories for authorId={}", authorId);
		return storyRepository.findByAuthorId(authorId).stream().map(storyMapper::toResponseDto)
				.collect(Collectors.toList());
	}

	// Called by {@link com.connectsphere.media.scheduler.StoryExpiryScheduler}.
	@Override
	public int expireOldStories() {
		LocalDateTime now = LocalDateTime.now();
		int expired = storyRepository.expireStoriesOlderThan(now);
		if (expired > 0) {
			logger.info("Expired {} stories at {}", expired, now);
		} else {
			logger.debug("No stories to expire at {}", now);
		}
		return expired;
	}

	// Private helpers

	// Validates the MIME type against the platform's allowed set. For story type
	private void validateMimeType(String mimeType) {
		if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
			throw new InvalidMediaTypeException(mimeType);
		}
	}
}
