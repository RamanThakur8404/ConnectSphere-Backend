package com.connectsphere.comment_service.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	 @Column(name = "comment_id")
	private Long commentId;

	@Column(name = "post_id", nullable = false)
	private Long postId;

	@Column(name = "author_id", nullable = false)
	private Long authorId;

	@Column(name = "parent_comment_id")
	private Long parentCommentId;

	@Column(name= "content", nullable = false, length= 1000)
	private String content;

	@Column(nullable = false, name= "likes_count")
	private Integer likesCount;

	@Column(nullable = false, name= "is_deleted")
	private Boolean isDeleted;

	@Column(nullable = false, name="created_at", updatable=false)
	private LocalDateTime createdAt;

	@Column(nullable = false, name="updated_at")
	private LocalDateTime updatedAt;

	@PrePersist
	public void prePersist() {
		this.createdAt = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();
		this.likesCount = 0;
		this.isDeleted = false;
	}

	@PreUpdate
	public void preUpdate() {
		this.updatedAt = LocalDateTime.now();
	}
}
