package com.connectsphere.auth.event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Event published to RabbitMQ when a new user registers.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent {

    private Long   userId;
    private String email;
    private String username;
    private String fullName;
}