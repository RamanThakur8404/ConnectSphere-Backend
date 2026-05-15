package com.connectsphere.media_service.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

// Unit tests for {@link MediaServiceImpl}.
@ExtendWith(MockitoExtension.class)
class MediaServiceImplTest {

    @Mock private MediaRepository mediaRepository;
    @Mock private StoryRepository storyRepository;
    @Mock private MediaMapper     mediaMapper;
    @Mock private StoryMapper     storyMapper;

    @InjectMocks
    private MediaServiceImpl mediaService;

    // Test fixtures

    private MediaRequestDto mediaRequestDto;
    private Media           mediaEntity;
    private MediaResponseDto mediaResponseDto;

    private StoryRequestDto  storyRequestDto;
    private Story            storyEntity;
    private StoryResponseDto storyResponseDto;

    @BeforeEach
    void setUp() {
        // Media fixtures
        mediaRequestDto = MediaRequestDto.builder()
                .uploaderId(1)
                .mediaTypes(MediaTypes.IMAGE)
                .sizeKb(512L)
                .mimeType("image/jpeg")
                .linkedPostId(10)
                .build();

        mediaEntity = Media.builder()
                .mediaId(100)
                .uploaderId(1)
                .url("https://cdn.connectsphere.io/media/test.jpg")
                .mediaTypes(MediaTypes.IMAGE)
                .sizeKb(512L)
                .mimeType("image/jpeg")
                .linkedPostId(10)
                .isDeleted(false)
                .build();

        mediaResponseDto = MediaResponseDto.builder()
                .mediaId(100)
                .uploaderId(1)
                .url("https://cdn.connectsphere.io/media/test.jpg")
                .mediaTypes(MediaTypes.IMAGE)
                .sizeKb(512L)
                .mimeType("image/jpeg")
                .linkedPostId(10)
                .isDeleted(false)
                .build();

        // Story fixtures
        storyRequestDto = StoryRequestDto.builder()
                .authorId(2)
                .mediaUrl("https://cdn.connectsphere.io/stories/vid.mp4")
                .caption("Test story")
                .mediaTypes(MediaTypes.VIDEO)
                .build();

        storyEntity = Story.builder()
                .storyId(200)
                .authorId(2)
                .mediaUrl("https://cdn.connectsphere.io/stories/vid.mp4")
                .caption("Test story")
                .mediaTypes(MediaTypes.VIDEO)
                .viewsCount(0)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .createdAt(LocalDateTime.now())
                .isActive(true)
                .build();

        storyResponseDto = StoryResponseDto.builder()
                .storyId(200)
                .authorId(2)
                .mediaUrl("https://cdn.connectsphere.io/stories/vid.mp4")
                .caption("Test story")
                .mediaTypes(MediaTypes.VIDEO)
                .viewsCount(0)
                .isActive(true)
                .build();
    }

    // uploadMedia

    @Test
    @DisplayName("uploadMedia: valid MIME type persists record and returns DTO")
    void uploadMedia_validMimeType_returnsResponseDto() {
        // Given
        when(mediaMapper.toEntity(mediaRequestDto)).thenReturn(mediaEntity);
        when(mediaRepository.save(mediaEntity)).thenReturn(mediaEntity);
        when(mediaMapper.toResponseDto(mediaEntity)).thenReturn(mediaResponseDto);

        // When
        MediaResponseDto result = mediaService.uploadMedia(
                mediaRequestDto, "https://cdn.connectsphere.io/media/test.jpg");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMediaId()).isEqualTo(100);
        assertThat(result.getMimeType()).isEqualTo("image/jpeg");
        verify(mediaRepository, times(1)).save(mediaEntity);
    }

    @Test
    @DisplayName("uploadMedia: unsupported MIME type throws InvalidMediaTypeException")
    void uploadMedia_invalidMimeType_throwsException() {
        // Given — dto with unsupported MIME type
        mediaRequestDto.setMimeType("application/pdf");

        // When / Then
        assertThatThrownBy(() -> mediaService.uploadMedia(mediaRequestDto, "someUrl"))
                .isInstanceOf(InvalidMediaTypeException.class)
                .hasMessageContaining("application/pdf");

        verify(mediaRepository, never()).save(any());
    }

    @Test
    @DisplayName("uploadMedia: PNG MIME type is accepted")
    void uploadMedia_pngMimeType_accepted() {
        mediaRequestDto.setMimeType("image/png");
        when(mediaMapper.toEntity(mediaRequestDto)).thenReturn(mediaEntity);
        when(mediaRepository.save(any())).thenReturn(mediaEntity);
        when(mediaMapper.toResponseDto(mediaEntity)).thenReturn(mediaResponseDto);

        assertThatCode(() -> mediaService.uploadMedia(mediaRequestDto, "url"))
                .doesNotThrowAnyException();
    }

    // getMediaById

    @Test
    @DisplayName("getMediaById: existing ID returns DTO")
    void getMediaById_exists_returnsDto() {
        when(mediaRepository.findByMediaId(100)).thenReturn(Optional.of(mediaEntity));
        when(mediaMapper.toResponseDto(mediaEntity)).thenReturn(mediaResponseDto);

        MediaResponseDto result = mediaService.getMediaById(100);

        assertThat(result.getMediaId()).isEqualTo(100);
    }

    @Test
    @DisplayName("getMediaById: non-existent ID throws MediaNotFoundException")
    void getMediaById_notFound_throwsException() {
        when(mediaRepository.findByMediaId(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mediaService.getMediaById(999))
                .isInstanceOf(MediaNotFoundException.class)
                .hasMessageContaining("999");
    }

    // getMediaByPost

    @Test
    @DisplayName("getMediaByPost: returns all media for a post")
    void getMediaByPost_returnsListForPost() {
        when(mediaRepository.findByLinkedPostId(10)).thenReturn(List.of(mediaEntity));
        when(mediaMapper.toResponseDto(mediaEntity)).thenReturn(mediaResponseDto);

        List<MediaResponseDto> result = mediaService.getMediaByPost(10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLinkedPostId()).isEqualTo(10);
    }

    // deleteMedia

    @Test
    @DisplayName("deleteMedia: existing ID performs soft-delete")
    void deleteMedia_exists_softDeletes() {
        when(mediaRepository.findByMediaId(100)).thenReturn(Optional.of(mediaEntity));

        mediaService.deleteMedia(100);

        verify(mediaRepository, times(1)).deleteByMediaId(100);
    }

    @Test
    @DisplayName("deleteMedia: non-existent ID throws MediaNotFoundException")
    void deleteMedia_notFound_throwsException() {
        when(mediaRepository.findByMediaId(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mediaService.deleteMedia(999))
                .isInstanceOf(MediaNotFoundException.class);

        verify(mediaRepository, never()).deleteByMediaId(anyInt());
    }

    // createStory

    @Test
    @DisplayName("createStory: valid VIDEO type persists story and returns DTO")
    void createStory_validVideoType_returnsDto() {
        when(storyMapper.toEntity(storyRequestDto)).thenReturn(storyEntity);
        when(storyRepository.save(storyEntity)).thenReturn(storyEntity);
        when(storyMapper.toResponseDto(storyEntity)).thenReturn(storyResponseDto);

        StoryResponseDto result = mediaService.createStory(storyRequestDto);

        assertThat(result.getStoryId()).isEqualTo(200);
        assertThat(result.getAuthorId()).isEqualTo(2);
        verify(storyRepository, times(1)).save(storyEntity);
    }

    @Test
    @DisplayName("createStory: IMAGE type is also accepted")
    void createStory_imageType_accepted() {
        storyRequestDto.setMediaTypes(MediaTypes.IMAGE);
        when(storyMapper.toEntity(storyRequestDto)).thenReturn(storyEntity);
        when(storyRepository.save(any())).thenReturn(storyEntity);
        when(storyMapper.toResponseDto(storyEntity)).thenReturn(storyResponseDto);

        assertThatCode(() -> mediaService.createStory(storyRequestDto))
                .doesNotThrowAnyException();
    }

    // getActiveStories

    @Test
    @DisplayName("getActiveStories: returns stories for given author IDs")
    void getActiveStories_returnsListForAuthors() {
        List<Integer> authorIds = List.of(1, 2, 3);
        when(storyRepository.findActiveStoriesByAuthorIds(authorIds))
                .thenReturn(List.of(storyEntity));
        when(storyMapper.toResponseDto(storyEntity)).thenReturn(storyResponseDto);

        List<StoryResponseDto> result = mediaService.getActiveStories(authorIds);

        assertThat(result).hasSize(1);
    }

    // viewStory

    @Test
    @DisplayName("viewStory: active story increments view count")
    void viewStory_activeStory_incrementsCount() {
        when(storyRepository.findActiveById(200)).thenReturn(Optional.of(storyEntity));

        mediaService.viewStory(200, 5);

        verify(storyRepository, times(1)).incrementViewCount(200);
    }

    @Test
    @DisplayName("viewStory: expired or absent story throws StoryNotFoundException")
    void viewStory_notFound_throwsException() {
        when(storyRepository.findActiveById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mediaService.viewStory(999, 5))
                .isInstanceOf(StoryNotFoundException.class)
                .hasMessageContaining("999");
    }

    // deleteStory

    @Test
    @DisplayName("deleteStory: active story is deactivated (soft-deleted)")
    void deleteStory_activeStory_deactivates() {
        when(storyRepository.findActiveById(200)).thenReturn(Optional.of(storyEntity));

        mediaService.deleteStory(200);

        verify(storyRepository, times(1)).deactivateStory(200);
    }

    @Test
    @DisplayName("deleteStory: non-existent story throws StoryNotFoundException")
    void deleteStory_notFound_throwsException() {
        when(storyRepository.findActiveById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mediaService.deleteStory(999))
                .isInstanceOf(StoryNotFoundException.class);
    }

    // getStoriesByUser

    @Test
    @DisplayName("getStoriesByUser: returns all active stories for author")
    void getStoriesByUser_returnsStoriesForAuthor() {
        when(storyRepository.findByAuthorId(2)).thenReturn(List.of(storyEntity));
        when(storyMapper.toResponseDto(storyEntity)).thenReturn(storyResponseDto);

        List<StoryResponseDto> result = mediaService.getStoriesByUser(2);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAuthorId()).isEqualTo(2);
    }

    // expireOldStories

    @Test
    @DisplayName("expireOldStories: delegates to repository and returns count")
    void expireOldStories_delegatesToRepository() {
        when(storyRepository.expireStoriesOlderThan(any(LocalDateTime.class))).thenReturn(3);

        int count = mediaService.expireOldStories();

        assertThat(count).isEqualTo(3);
        verify(storyRepository, times(1)).expireStoriesOlderThan(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("expireOldStories: returns zero when no stories expired")
    void expireOldStories_nothingToExpire_returnsZero() {
        when(storyRepository.expireStoriesOlderThan(any(LocalDateTime.class))).thenReturn(0);

        int count = mediaService.expireOldStories();

        assertThat(count).isZero();
    }
}
