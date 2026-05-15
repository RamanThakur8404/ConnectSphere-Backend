package com.connectsphere.auth.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {

    private Long userId;
    private String username;
    private String email;
    private String fullName;
    private String bio;
    private String profilePicUrl;
    private String role;
    private String        provider;
    private boolean       active;
    private boolean       isPremium;
    private boolean       twoFactorEnabled;
    private boolean       emailVerified;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}
