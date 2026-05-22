package com.connectsphere.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class LoginRequest {

	@NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
	@Pattern(
		    regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$",
		    message = "Email must follow standard format (example: user@gmail.com)"
		)
	private String email;

    @NotBlank(message = "Password is required")
    private String password;
}