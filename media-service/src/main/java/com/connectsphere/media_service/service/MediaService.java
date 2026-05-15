package com.connectsphere.media_service.service;

import java.util.List;

import com.connectsphere.media_service.dto.MediaRequestDto;
import com.connectsphere.media_service.dto.MediaResponseDto;
import com.connectsphere.media_service.dto.StoryRequestDto;
import com.connectsphere.media_service.dto.StoryResponseDto;

// Service contract for the Media/Story domain.
public interface MediaService {

    // Media operations

    // Stores metadata for a newly uploaded media file.
    MediaResponseDto uploadMedia(MediaRequestDto dto, String cdnUrl);

    // Returns all active media records attached to a specific post.
    List<MediaResponseDto> getMediaByPost(int postId);

    // Returns a single media record by its ID.
    MediaResponseDto getMediaById(int mediaId);

    // Soft-deletes a media record.
    void deleteMedia(int mediaId);

    // Story operations

    // Creates a new 24-hour story for the given user.
    StoryResponseDto createStory(StoryRequestDto dto);

    // Returns all currently active (non-expired) stories from a list of user IDs.
    List<StoryResponseDto> getActiveStories(List<Integer> authorIds);

    // Records a story view for the given user and increments the story's view count.
    void viewStory(int storyId, int viewerId);

    // Soft-deletes a story (sets isActive = false).
    void deleteStory(int storyId);

    // Returns all active stories created by a specific user.
    List<StoryResponseDto> getStoriesByUser(int authorId);

    // Expires all stories whose 24-hour window has elapsed.
    int expireOldStories();
}
