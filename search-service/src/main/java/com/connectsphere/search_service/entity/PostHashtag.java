package com.connectsphere.search_service.entity;

import java.time.LocalDateTime;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// Many-to-many join entity between Posts and Hashtags.
@Entity
@Table(name = "post_hashtags", uniqueConstraints = @UniqueConstraint(name = "uq_post_hashtag", columnNames = {
		"post_id", "hashtag_id" }), indexes = { @Index(name = "idx_ph_post_id", columnList = "post_id"),
				@Index(name = "idx_ph_hashtag_id", columnList = "hashtag_id") })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = { "hashtag" })
public class PostHashtag {

	// Surrogate primary key. 
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	// Foreign key referencing the post in the post-service schema. Stored as a
	@Column(name = "post_id", nullable = false)
	private Integer postId;

	// The associated hashtag record in this service's schema. Eager fetch
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "hashtag_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ph_hashtag"))
	private Hashtag hashtag;

	// Timestamp when this post-hashtag mapping was created. 
	@Column(name = "created_at", nullable = false, updatable = false)
	@Builder.Default
	private LocalDateTime createdAt = LocalDateTime.now();
}
