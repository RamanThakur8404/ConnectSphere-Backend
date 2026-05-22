package com.connectsphere.comment_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentUpdateDTO {
	@NotBlank(message = "Updated content must not be blank")
    @Size(min = 1, max = 1000, message = "Content must be between 1 and 1000 characters")
    private String content;
}
