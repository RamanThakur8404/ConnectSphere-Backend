package com.connectsphere.like_service.dto;

import com.connectsphere.like_service.constant.ReactionType;
import com.connectsphere.like_service.constant.TargetType;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LikeRequestDTO {
	@NotNull(message = "User ID is required")
    private Integer userId;

    @NotNull(message = "Target ID is required")
    private Integer targetId;

    @NotNull(message = "Target type is required")
    private TargetType targetType;

    @NotNull(message = "Reaction type is required")
    private ReactionType reactionType;

}
