package com.connectsphere.notification_service.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent {

    // Database ID of the newly registered user. 
    private Long userId;

    // Email address — used to send the welcome email. 
    private String email;

    // Chosen username — used in the welcome notification message. 
    private String username;

    // Full name — used in the welcome email greeting. 
    private String fullName;
}