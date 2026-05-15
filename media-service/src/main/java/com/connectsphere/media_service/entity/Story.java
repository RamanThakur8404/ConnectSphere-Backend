package com.connectsphere.media_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;

import com.connectsphere.media_service.constants.MediaTypes;

// Entity representing an ephemeral Story.
@Entity
@Table(name = "story", indexes = {
    @Index(name = "idx_story_author_id", columnList = "author_id"),
    @Index(name = "idx_story_expires_at", columnList = "expires_at"),
    @Index(name = "idx_story_is_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "storyId")
public class Story {

    // Auto-incremented primary key. 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "story_id")
    private Integer storyId;

    // ID of the user who created this story. 
    @NotNull(message = "Author ID must not be null")
    @Column(name = "author_id", nullable = false)
    private Integer authorId;

    // CDN / S3 URL of the story media file. 
    @NotBlank(message = "Media URL must not be blank")
    @Column(name = "media_url", nullable = false, length = 1024)
    private String mediaUrl;

    // Optional caption text for the story (max 500 chars). 
    @Column(name = "caption", length = 500)
    private String caption;

    // IMAGE or VIDEO — indicates content type of the story. 
    @NotNull(message = "Media type must not be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 10)
    private MediaTypes mediaTypes;

    // Number of times this story has been viewed by other users.
    @Column(name = "views_count", nullable = false)
    @Builder.Default
    private Integer viewsCount = 0;

    // Expiry timestamp — exactly 24 hours after creation.
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    // Timestamp when the story was created. Set automatically before persist. 
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Active flag — false once the story is expired or manually deleted.
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // Populates createdAt and expiresAt (24-hour window)
    @PrePersist
    public void prePersist() {
        this.createdAt  = LocalDateTime.now();
        this.expiresAt  = this.createdAt.plusHours(24);
    }
}
