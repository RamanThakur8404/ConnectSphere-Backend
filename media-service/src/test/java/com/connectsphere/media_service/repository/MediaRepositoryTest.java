package com.connectsphere.media_service.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.connectsphere.media_service.constants.MediaTypes;
import com.connectsphere.media_service.entity.Media;

@DataJpaTest
@ActiveProfiles("test")
class MediaRepositoryTest {

    @Autowired
    private MediaRepository mediaRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Media savedMedia;

    @BeforeEach
    void setUp() {
        // Persist a fresh media record before each test
        Media media = Media.builder()
                .uploaderId(1)
                .url("https://cdn.connectsphere.io/media/sample.jpg")
                .mediaTypes(MediaTypes.IMAGE)
                .sizeKb(256L)
                .mimeType("image/jpeg")
                .linkedPostId(10)
                .isDeleted(false)
                .build();
        savedMedia = mediaRepository.save(media);
    }

    // findByMediaId

    @Test
    @DisplayName("findByMediaId: returns present Optional for existing non-deleted record")
    void findByMediaId_notDeleted_returnsPresent() {
        Optional<Media> result = mediaRepository.findByMediaId(savedMedia.getMediaId());

        assertThat(result).isPresent();
        assertThat(result.get().getUploaderId()).isEqualTo(1);
    }

    @Test
    @DisplayName("findByMediaId: returns empty Optional after soft-delete")
    void findByMediaId_afterSoftDelete_returnsEmpty() {
        mediaRepository.deleteByMediaId(savedMedia.getMediaId());
        entityManager.flush();
        entityManager.clear();

        Optional<Media> result = mediaRepository.findByMediaId(savedMedia.getMediaId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByMediaId: returns empty Optional for unknown ID")
    void findByMediaId_unknownId_returnsEmpty() {
        Optional<Media> result = mediaRepository.findByMediaId(9999);

        assertThat(result).isEmpty();
    }

    // findByLinkedPostId

    @Test
    @DisplayName("findByLinkedPostId: returns media for the correct post")
    void findByLinkedPostId_matchingPost_returnsList() {
        List<Media> result = mediaRepository.findByLinkedPostId(10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLinkedPostId()).isEqualTo(10);
    }

    @Test
    @DisplayName("findByLinkedPostId: returns empty list for unlinked post")
    void findByLinkedPostId_noMatch_returnsEmptyList() {
        List<Media> result = mediaRepository.findByLinkedPostId(999);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByLinkedPostId: excludes soft-deleted media")
    void findByLinkedPostId_softDeleted_excludedFromResults() {
        mediaRepository.deleteByMediaId(savedMedia.getMediaId());
        entityManager.flush();
        entityManager.clear();

        List<Media> result = mediaRepository.findByLinkedPostId(10);

        assertThat(result).isEmpty();
    }

    // findByUploaderId

    @Test
    @DisplayName("findByUploaderId: returns all non-deleted media for a user")
    void findByUploaderId_returnsMediaForUser() {
        List<Media> result = mediaRepository.findByUploaderId(1);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("findByUploaderId: excludes soft-deleted records")
    void findByUploaderId_softDeleted_excluded() {
        mediaRepository.deleteByMediaId(savedMedia.getMediaId());
        entityManager.flush();
        entityManager.clear();

        List<Media> result = mediaRepository.findByUploaderId(1);

        assertThat(result).isEmpty();
    }

    // deleteByMediaId (soft-delete)

    @Test
    @DisplayName("deleteByMediaId: physical row still exists after soft-delete")
    void deleteByMediaId_rowRetainedAfterSoftDelete() {
        mediaRepository.deleteByMediaId(savedMedia.getMediaId());
        entityManager.flush();
        entityManager.clear();

        // Physical row exists (standard JPA find, bypasses our custom query)
        Optional<Media> raw = mediaRepository.findById(savedMedia.getMediaId());
        assertThat(raw).isPresent();
        assertThat(raw.get().getIsDeleted()).isTrue();
    }

    // findByMediaType

    @Test
    @DisplayName("findByMediaType: returns matching IMAGE records for uploader")
    void findByMediaType_image_returnsMatch() {
        List<Media> result = mediaRepository.findByMediaType(1, MediaTypes.IMAGE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMediaTypes()).isEqualTo(MediaTypes.IMAGE);
    }

    @Test
    @DisplayName("findByMediaType: returns empty list when type does not match")
    void findByMediaType_noMatch_returnsEmpty() {
        List<Media> result = mediaRepository.findByMediaType(1, MediaTypes.VIDEO);

        assertThat(result).isEmpty();
    }
}