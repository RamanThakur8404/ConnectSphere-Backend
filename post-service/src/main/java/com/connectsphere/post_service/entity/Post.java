package com.connectsphere.post_service.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.connectsphere.post_service.constants.PostType;
import com.connectsphere.post_service.constants.Visibility;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.JoinColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "posts")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Post {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "post_id")
	private int postId;

	@Column(nullable = false)
	private int authorId;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "post_media_urls", joinColumns = @JoinColumn(name = "post_id"))
	@Column(name = "media_url")
	@Builder.Default
	private List<String> mediaUrls = new ArrayList<>();

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "post_tags", joinColumns = @JoinColumn(name = "post_id"))
	@Column(name = "tag", length = 100)
	@Builder.Default
	private List<String> hashtags = new ArrayList<>();

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PostType postType;

	@Enumerated(EnumType.STRING)
	@Builder.Default
	private Visibility visibility= Visibility.PUBLIC;

	@Column(nullable = false)
	private int likesCount;

	@Column(nullable = false)
	private int commentsCount;

	@Column(nullable = false)
	private int sharesCount;
	
	// Repost reference
    @Column(name = "original_post_id")
    private Integer originalPostId;

    @Column(nullable = false)
    @Builder.Default
    private boolean contentWarning = false;

    @Column(name = "scheduled_publish_at")
    private LocalDateTime scheduledPublishAt;
    
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	@Column(nullable = false)
	@Builder.Default
	private boolean isDeleted = false;

	@PrePersist
	protected void onCreate() {
		this.createdAt = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();
		this.likesCount = 0;
		this.commentsCount = 0;
		this.sharesCount = 0;
		
		if (this.visibility == null) {
            this.visibility = Visibility.PUBLIC;
        }
	}

	@PreUpdate
	protected void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}
}
