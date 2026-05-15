package com.connectsphere.notification_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailAlertRequest {

	@NotBlank(message = "Recipient email must not be blank.")
	@Email(message = "Recipient email must be a valid address.")
	@Pattern(
		regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$",
		message = "Recipient email must follow standard format (example: user@gmail.com)."
	)
	private String toEmail;

	@NotBlank(message = "Subject must not be blank.")
	@Size(max = 200, message = "Subject must not exceed 200 characters.")
	private String subject;

	@NotBlank(message = "Email body must not be blank.")
	@Size(max = 5000, message = "Email body must not exceed 5000 characters.")
	private String body;
}
