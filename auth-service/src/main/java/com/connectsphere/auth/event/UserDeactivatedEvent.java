package com.connectsphere.auth.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Event published to RabbitMQ when an admin deactivates a user account.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDeactivatedEvent {

   private Long   userId;
   private String email;
   private String deactivatedByAdminEmail;
}
