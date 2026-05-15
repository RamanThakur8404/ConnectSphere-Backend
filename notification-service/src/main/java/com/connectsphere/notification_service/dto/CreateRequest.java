package com.connectsphere.notification_service.dto;

import com.connectsphere.notification_service.constants.NotificationTarget;
import com.connectsphere.notification_service.constants.NotificationType;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRequest {

        // ID of the user who will receive the notification. 
        @NotNull(message = "Recipient ID must not be null.")
        private Integer recipientId;

        // ID of the user whose action triggered the notification. 
        @NotNull(message = "Actor ID must not be null.")
        private Integer actorId;

        // Type of notification event.
        @NotNull(message = "Notification type must not be null.")
        private NotificationType type;

        // Human-readable message to display in the notification feed.
        @NotBlank(message = "Message must not be blank.")
        @Size(min = 1, max = 500, message = "Message must be between 1 and 500 characters.")
        private String message;

        // Optional ID of the target entity (post or comment). 
        private Integer targetId;

        // Optional type of the target entity.
        private NotificationTarget targetType;

        // Optional deep-link URL for navigation on click. 
        @Size(max = 300, message = "Deep-link URL must not exceed 300 characters.")
        private String deepLinkUrl;
}

   