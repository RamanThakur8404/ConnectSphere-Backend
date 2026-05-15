package com.connectsphere.media_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import java.time.LocalDateTime;

import com.connectsphere.media_service.constants.MediaTypes;

// Entity representing an uploaded media file (image or video).
@Entity
@Table(name = "media", indexes = {
    @Index(name = "idx_media_uploader_id", columnList = "uploader_id"),
    @Index(name = "idx_media_linked_post_id", columnList = "linked_post_id"),
    @Index(name = "idx_media_is_deleted", columnList = "is_deleted")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "url")
@EqualsAndHashCode(of = "mediaId")
public class Media {

    // Auto-incremented primary key. 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "media_id")
    private Integer mediaId;

    // ID of the user who uploaded this media. 
    @NotNull(message = "Uploader ID must not be null")
    @Column(name = "uploader_id", nullable = false)
    private Integer uploaderId;

    // CDN / S3 URL where the file is hosted. 
    @NotBlank(message = "Media URL must not be blank")
    @Column(name = "url", nullable = false, length = 1024)
    private String url;

    // IMAGE or VIDEO — stored as a string column for readability. 
    @NotNull(message = "Media type must not be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 10)
    private MediaTypes mediaTypes;

    // File size in kilobytes. 
    @Positive(message = "File size must be a positive value")
    @Column(name = "size_kb", nullable = false)
    private Long sizeKb;

    // MIME type string, e.g. image/jpeg, video/mp4. 
    @NotBlank(message = "MIME type must not be blank")
    @Column(name = "mime_type", nullable = false, length = 50)
    private String mimeType;

    // Post to which this media is attached.
    @Column(name = "linked_post_id")
    private Integer linkedPostId;

    // Timestamp recorded automatically on first insert. 
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    // Soft-delete flag — true = logically deleted but retained for audit.
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    // Populates uploadedAt with the current timestamp before persistence. 
    @PrePersist
    public void prePersist() {
        this.uploadedAt = LocalDateTime.now();
    }
}
