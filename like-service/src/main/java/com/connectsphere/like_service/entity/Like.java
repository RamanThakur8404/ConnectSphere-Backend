package com.connectsphere.like_service.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.connectsphere.like_service.constant.ReactionType;
import com.connectsphere.like_service.constant.TargetType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "likes", 
	   uniqueConstraints = {
		   @UniqueConstraint(
		       name = "uq_user_target",
		       columnNames = {"user_id", "target_id", "target_type"}
		    )
		},
		indexes = {
		  @Index(name = "idx_target_id_type", columnList = "target_id"),
		  @Index(name = "idx_user_id",        columnList = "user_id"),
		  @Index(name = "idx_target_type", columnList = "target_type")
		})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Like {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "like_id")
	private Integer likeId;

	@Column(name = "user_id", nullable = false)
	private Integer userId;

	@Column(name = "target_id", nullable = false)
	private Integer targetId;

	@Column(name = "target_type", nullable = false)
	@Enumerated(EnumType.STRING)
	private TargetType targetType;

	@Column(name = "reaction_type", nullable = false)
	@Enumerated(EnumType.STRING)
	private ReactionType reactionType;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;
	
}
