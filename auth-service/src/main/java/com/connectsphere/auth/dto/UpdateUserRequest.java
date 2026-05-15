package com.connectsphere.auth.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {
	@Size(min = 3, max = 30, message = "Username must be between 3 and 30 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9_]*$",
        message = "Username may only contain letters, digits and underscores"
    )
    private String username;

    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;

    @Size(max = 300, message = "Bio must not exceed 300 characters")
    private String bio;

    @Pattern(
        regexp = "^(https?://.*)?$",
        message = "Profile picture URL must be a valid http/https URL or empty"
    )
    private String profilePicUrl;
}