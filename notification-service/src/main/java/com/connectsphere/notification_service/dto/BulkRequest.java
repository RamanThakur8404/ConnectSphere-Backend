package com.connectsphere.notification_service.dto;

import java.util.List;

import com.connectsphere.notification_service.constants.NotificationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkRequest {

	// List of recipient user IDs. Must contain at least one entry and no more than
	@NotEmpty(message = "Recipient list must not be empty.")
	@Size(max = 500, message = "Bulk dispatch supports a maximum of 500 recipients per call.")
	private List<@NotNull(message = "Recipient ID must not be null.") @Positive(message = "Recipient ID must be a positive number.") Integer> recipientIds;

	// ID of the actor or system account triggering the broadcast. 
	@NotNull(message = "Actor ID must not be null.")
	private Integer actorId;

	// Notification type — invalid enum values are rejected with a 400. 
	@NotNull(message = "Notification type must not be null.")
	private NotificationType type;

	// Broadcast message text. 
	@NotBlank(message = "Message must not be blank.")
	@Size(min = 1, max = 500, message = "Message must be between 1 and 500 characters.")
	private String message;
}
